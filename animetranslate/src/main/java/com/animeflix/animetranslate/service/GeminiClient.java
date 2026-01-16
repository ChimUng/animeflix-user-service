package com.animeflix.animetranslate.service;

import com.animeflix.animetranslate.exception.GeminiApiException;
import com.animeflix.animetranslate.exception.ParseTranslationException;
import com.animeflix.animetranslate.model.TranslationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final Pattern PARSE_PATTERN = Pattern.compile("Tên:\\s*(.+?)\\nMô tả:\\s*(.+)", Pattern.DOTALL);

    private final WebClient geminiWebClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiClient(WebClient geminiWebClient) {
        this.geminiWebClient = requireNonNull(geminiWebClient);
    }
//    Hàm gửi resquest đã remake format từ PromtBuilder -> lên geminibot + apikey để quoto
    public Mono<List<TranslationResponse>> translate(List<Map<String, String>> prompts) {
        return Flux.fromIterable(prompts)
                .flatMap(this::callGeminiForPrompt)
                .collectList()
                .retryWhen(Retry.backoff(3, java.time.Duration.ofMillis(1000))
                        .filter(t -> t instanceof WebClientResponseException && ((WebClientResponseException) t).getStatusCode().value() == 429))
                .doOnError(e -> log.error("Gemini batch translation failed", e));
    }

    private Mono<TranslationResponse> callGeminiForPrompt(Map<String, String> prompt) {
        Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt.get("prompt"))))));
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;


        return geminiWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> parseResponse(response, prompt))
                .onErrorResume(t -> handleError(t, prompt));
    }

    private TranslationResponse parseResponse(Map<String, Object> response, Map<String, String> prompt) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new ParseTranslationException("No candidates found in Gemini response");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");

            Matcher match = PARSE_PATTERN.matcher(text);
            TranslationResponse resp = new TranslationResponse();
            resp.setAnilistId(Integer.valueOf(requireNonNull(prompt.get("anilistId"))));  // Lưu ý: prompt cần accessible, hoặc pass anilistId riêng

            if (match.matches()) {
                resp.setTitleVi(match.group(1).trim());
                resp.setDescriptionVi(match.group(2).trim());
                log.debug("Parsed translation for ID {}: {}", resp.getAnilistId(), resp.getTitleVi());
            } else {
                throw new ParseTranslationException("Invalid format: " + text);
            }
            return resp;
        } catch (Exception e) {
            throw new ParseTranslationException("Response parse error", e);
        }
    }

    private Mono<TranslationResponse> handleError(Throwable t, Map<String, String> prompt) {
        log.warn("Gemini call failed for prompt: {}", t.getMessage());
        TranslationResponse resp = new TranslationResponse();
        resp.setAnilistId(Integer.valueOf(prompt.get("anilistId")));
        resp.setError("Gemini call failed: " + t.getMessage());
        return Mono.just(resp);
    }
}