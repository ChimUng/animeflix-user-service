package com.animeflix.aisearchservice.service;

import com.animeflix.aisearchservice.client.GeminiClient;
import com.animeflix.aisearchservice.dto.response.ParsedQueryDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryParserService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Value("${search.parser.confidence-threshold}")
    private double confidenceThreshold;

    /**
     * System prompt cứng - ràng buộc Gemini chỉ dùng enum có sẵn.
     * KHÔNG để Gemini tự tạo genre/status mới.
     */
    private static final String SYSTEM_PROMPT = """
            Bạn là bộ phân tích query tìm kiếm anime. Nhiệm vụ: chuyển câu hỏi tự nhiên thành JSON filter.
            
            QUY TẮC BẮT BUỘC:
            1. Chỉ dùng các giá trị trong enum bên dưới, KHÔNG tự tạo giá trị mới
            2. Nếu không thể map sang enum → đặt fallbackToEmbedding = true, confidence thấp
            3. Trả về JSON thuần túy, KHÔNG có markdown hay backtick
            
            GENRES hợp lệ (chỉ dùng đúng tên này):
            Action, Adventure, Comedy, Drama, Ecchi, Fantasy, Horror, Mahou Shoujo,
            Mecha, Music, Mystery, Psychological, Romance, Sci-Fi, Slice of Life,
            Sports, Supernatural, Thriller, School, Harem, Isekai, Josei, Kids,
            Seinen, Shoujo, Shounen
            
            TAGS hợp lệ (chọn nếu phù hợp):
            Amnesia, Time Travel, Reincarnation, Overpowered Main Character,
            School Life, Love Triangle, Revenge, Demons, Vampires, Military,
            Magic, Super Power, Virtual Reality, Male Protagonist, Female Protagonist,
            Childhood Friends, Teacher-Student Relationship, Office Romance
            
            FORMAT hợp lệ: TV | MOVIE | OVA | ONA | SPECIAL | MUSIC
            STATUS hợp lệ: RELEASING | FINISHED | NOT_YET_RELEASED | CANCELLED | HIATUS
            SEASON hợp lệ: WINTER | SPRING | SUMMER | FALL
            SORT hợp lệ: POPULARITY_DESC | SCORE_DESC | TRENDING_DESC | FAVOURITES_DESC | START_DATE_DESC
            
            OUTPUT FORMAT (bắt buộc đúng schema này):
            {
              "genres": [],
              "tags": [],
              "format": null,
              "status": null,
              "season": null,
              "seasonYear": null,
              "sort": ["POPULARITY_DESC"],
              "confidence": 0.0,
              "fallbackToEmbedding": false,
              "reasoning": "giải thích ngắn"
            }
            
            VÍ DỤ:
            Query: "anime hài hước học đường đang chiếu"
            → {"genres":["Comedy","School"],"status":"RELEASING","sort":["TRENDING_DESC"],"confidence":0.92,"fallbackToEmbedding":false,"reasoning":"Hài hước→Comedy, Học đường→School, Đang chiếu→RELEASING"}
            
            Query: "anime nhân vật bị mất trí nhớ nhưng sau đó yêu lại người cũ"
            → {"genres":["Romance","Drama"],"tags":["Amnesia"],"confidence":0.55,"fallbackToEmbedding":true,"reasoning":"Mất trí nhớ→Amnesia tag, nhưng plot cụ thể không map đủ → cần embedding"}
            
            Query: "anime kinh dị tâm lý tối tăm"
            → {"genres":["Horror","Psychological"],"sort":["SCORE_DESC"],"confidence":0.88,"fallbackToEmbedding":false,"reasoning":"Kinh dị→Horror, Tâm lý→Psychological"}
            """;

    /**
     * Parse user query → ParsedQueryDTO
     * Gemini trả JSON → validate confidence → quyết định dùng path nào
     */
    public Mono<ParsedQueryDTO> parse(String userQuery) {
        log.info("🔍 Parsing query: '{}'", userQuery);

        return geminiClient.chat(SYSTEM_PROMPT, userQuery)
                .map(jsonText -> {
                    try {
                        // Strip markdown nếu Gemini vẫn wrap
                        String clean = jsonText
                                .replace("```json", "")
                                .replace("```", "")
                                .trim();

                        ParsedQueryDTO parsed = objectMapper.readValue(clean, ParsedQueryDTO.class);

                        // Validate và set default
                        if (parsed.getConfidence() == null) {
                            parsed.setConfidence(0.5);
                        }
                        if (parsed.getFallbackToEmbedding() == null) {
                            parsed.setFallbackToEmbedding(parsed.getConfidence() < confidenceThreshold);
                        }
                        // Override: nếu confidence thấp, bắt buộc fallback
                        if (parsed.getConfidence() < confidenceThreshold) {
                            parsed.setFallbackToEmbedding(true);
                        }

                        log.info("✅ Parsed: genres={}, confidence={}, fallback={}",
                                parsed.getGenres(), parsed.getConfidence(), parsed.getFallbackToEmbedding());

                        return parsed;

                    } catch (Exception e) {
                        log.error("❌ JSON parse error from Gemini: {}", jsonText, e);
                        // Parse thất bại → fallback embedding
                        ParsedQueryDTO fallback = new ParsedQueryDTO();
                        fallback.setConfidence(0.0);
                        fallback.setFallbackToEmbedding(true);
                        fallback.setReasoning("JSON parse error - fallback to embedding");
                        return fallback;
                    }
                })
                .onErrorReturn(createFallbackQuery());
    }

    private ParsedQueryDTO createFallbackQuery() {
        ParsedQueryDTO fallback = new ParsedQueryDTO();
        fallback.setConfidence(0.0);
        fallback.setFallbackToEmbedding(true);
        fallback.setReasoning("Gemini API error - fallback to embedding");
        return fallback;
    }
}