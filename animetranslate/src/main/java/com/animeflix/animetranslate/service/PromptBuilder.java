package com.animeflix.animetranslate.service;

import com.animeflix.animetranslate.model.TranslationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Component
public class PromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(PromptBuilder.class);
    private static final Pattern SEASON_PATTERN = Pattern.compile("(season|mùa|s|phần)[\\s\\-]*(\\d+)", Pattern.CASE_INSENSITIVE);

//    Hàm bốc tách list TranslateResponse dạng en thành aniId,title,description sau đó gửi cho GeminiClient để thực hiện tiếp
//    Hàm stream() để gom data đầu vào(transresquest) thành một object chứa nhiều requests
    public List<Map<String, String>> build(List<TranslationRequest> requests) {
        return requests.stream()
                .map(this::buildPromptForRequest)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, String> buildPromptForRequest(TranslationRequest req) {
        requireNonNull(req.getTitle(), "Title cannot be null");
        requireNonNull(req.getDescription(), "Description cannot be null");

        String lower = (req.getTitle() + " " + req.getDescription()).toLowerCase();
        String seasonText = extractSeason(lower);

        Map<String, String> promptMap = new HashMap<>();
        promptMap.put("anilistId", req.getAnilistId().toString());
        promptMap.put("title", req.getTitle());
        promptMap.put("description", req.getDescription());
        promptMap.put("prompt", buildPrompt(req.getTitle(), req.getDescription(), seasonText));

        return promptMap;
    }

    private String extractSeason(String lower) {
        Matcher seasonMatch = SEASON_PATTERN.matcher(lower);
        if (seasonMatch.find()) {
            return " Mùa " + seasonMatch.group(2);
        }
        return "";
    }

    private String buildPrompt(String title, String description, String seasonText) {
        return """
                Bạn là biên tập viên vietsub chuyên nghiệp cho cộng đồng anime.
                Hãy dịch tự nhiên tên và mô tả sang tiếng Việt theo định dạng sau:
                - Tên: <Tên tiếng Việt hoặc Romaji - Tên tiếng Việt%s>
                - Mô tả: <Mô tả dịch tự nhiên>

                YÊU CẦU BẮT BUỘC:
                1. Tên chỉ dùng một trong các định dạng:
                   - <Romaji> - <Tên tiếng Việt>%s
                   - <Tên tiếng Việt>%s
                   - <Romaji>%s
                2. KHÔNG chứa từ 'hoặc', ngoặc () hay giải thích phụ.
                3. Nếu tên anime phổ biến tại Việt Nam, dùng tên thường dùng (VD: One Piece → Vua Hải Tặc).
                4. Mô tả phải tự nhiên, dễ hiểu, không lặp từ, không máy móc.
                5. Bắt buộc trả về đúng định dạng:
                   Tên: <tên dịch>
                   Mô tả: <mô tả dịch>

                DỮ LIỆU:
                Tên: %s
                Mô tả: %s
                """.formatted(seasonText, seasonText, seasonText, seasonText, title, description);
    }
}
