package com.animeflix.aisearchservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.embedding-model}")
    private String embeddingModel;

    @Value("${gemini.api.chat-model}")
    private String chatModel;

    /**
     * Embed một đoạn text → List<Double> (768 dims)
     * Dùng cho: embed query của user và embed description anime
     */
    public Mono<List<Double>> embed(String text) {
        String url = "/models/" + embeddingModel + ":embedContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "model", "models/" + embeddingModel,
                "content", Map.of(
                        "parts", List.of(Map.of("text", text))
                )
        );

        return geminiWebClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode valuesNode = response
                            .path("embedding")
                            .path("values");

                    List<Double> vector = new ArrayList<>();
                    valuesNode.elements().forEachRemaining(v -> vector.add(v.asDouble()));

                    log.debug("✅ Embedded text, vector size: {}", vector.size());
                    return vector;
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .doBeforeRetry(signal -> log.warn("⚠️ Retrying Gemini embed, attempt: {}",
                                signal.totalRetries() + 1)))
                .doOnError(e -> log.error("❌ Gemini embed error: {}", e.getMessage()));
    }

    /**
     * Gọi Gemini Chat để parse query thành JSON filter.
     * Trả về raw text JSON từ Gemini.
     */
    public Mono<String> chat(String systemPrompt, String userMessage) {
        String url = "/models/" + chatModel + ":generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(
                        Map.of("role", "user",
                                "parts", List.of(Map.of("text", userMessage)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,           // Thấp → ổn định, ít sáng tạo
                        "responseMimeType", "application/json"  // Bắt buộc trả JSON
                )
        );

        return geminiWebClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    String text = response
                            .path("candidates")
                            .path(0)
                            .path("content")
                            .path("parts")
                            .path(0)
                            .path("text")
                            .asText();

                    log.debug("✅ Gemini chat response: {}", text);
                    return text;
                })
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                        .doBeforeRetry(signal -> log.warn("⚠️ Retrying Gemini chat, attempt: {}",
                                signal.totalRetries() + 1)))
                .doOnError(e -> log.error("❌ Gemini chat error: {}", e.getMessage()));
    }
}