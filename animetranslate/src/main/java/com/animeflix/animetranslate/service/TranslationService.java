package com.animeflix.animetranslate.service;

import com.animeflix.animetranslate.config.TranslateConfig;
import com.animeflix.animetranslate.exception.ValidationException;
import com.animeflix.animetranslate.model.TranslationRequest;
import com.animeflix.animetranslate.model.TranslationResponse;
import com.animeflix.animetranslate.repository.TranslationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);
    private static final int MAX_BATCH_SIZE = 10;

    private final WebClient geminiWebClient;
    private final TranslateConfig config;
    private final TranslationRepository repository;
    private final PromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final TranslationValidator validator;

    public TranslationService(@Qualifier("geminiWebClient") WebClient geminiWebClient,
                              TranslateConfig config,
                              TranslationRepository repository,
                              PromptBuilder promptBuilder,
                              GeminiClient geminiClient,
                              TranslationValidator validator) {
        this.geminiWebClient = geminiWebClient;
        this.config = config;
        this.repository = repository;
        this.promptBuilder = promptBuilder;
        this.geminiClient = geminiClient;
        this.validator = validator;
    }

    public Mono<List<TranslationResponse>> translateBatch(List<TranslationRequest> batch) {
        if (batch.isEmpty()) {
            log.debug("Empty batch, returning empty list.");
            return Mono.just(List.of());
        }

        List<Integer> anilistIds = batch.stream().map(TranslationRequest::getAnilistId).toList();
        return repository.getCachedTranslations(anilistIds)
                .flatMap(cached -> processCachedAndTranslate(cached, batch))
                .doOnSuccess(result -> log.info("Batch translation completed for {} items.", batch.size()));
    }

    private Mono<List<TranslationResponse>> processCachedAndTranslate(List<TranslationResponse> cached, List<TranslationRequest> batch) {
        Map<Integer, TranslationResponse> cachedMap = cached.stream()
                .collect(Collectors.toMap(TranslationResponse::getAnilistId, r -> r));

        List<TranslationRequest> toTranslate = batch.stream()
                .filter(req -> needsTranslation(req, cachedMap))
                .toList();

        if (toTranslate.isEmpty()) {
            return Mono.just(mapToResponses(batch, cachedMap));
        }

        List<TranslationRequest> trimmed = trimBatch(toTranslate);
        List<Map<String, String>> prompts = promptBuilder.build(trimmed);
        return geminiClient.translate(prompts)
                .flatMap(translations -> {  // FlatMap để chain upsert (Mono<Void> -> Mono.just(results))
                    return validator.upsertIfValid(validator.filterValid(translations))
                            .then(Mono.just(mergeResults(translations, cachedMap, batch)));
                })
                .onErrorResume(ValidationException.class, e -> {
                    log.error("Validation/upsert failed: {}", e.getMessage(), e);
                    return Mono.just(createErrorResponses(batch));
                });
    }

    private boolean needsTranslation(TranslationRequest req, Map<Integer, TranslationResponse> cachedMap) {
        TranslationResponse cachedItem = cachedMap.get(req.getAnilistId());
        if (cachedItem == null) return true;
        // Sử dụng validator's regex nếu cần, nhưng giữ đơn giản
        String desc = cachedItem.getDescriptionVi() != null ? cachedItem.getDescriptionVi().trim() : "";
        return desc.matches("^(?i)\\$\\$ ?(Chưa có|Không có|Hiện chưa có) \\s* mô tả \\.? \\$\\$? $");  // Inline regex cho quick check
    }

    private List<TranslationRequest> trimBatch(List<TranslationRequest> toTranslate) {
        if (toTranslate.size() > MAX_BATCH_SIZE) {
            log.warn("Batch size too large ({}), trimming to {}", toTranslate.size(), MAX_BATCH_SIZE);
            return toTranslate.subList(0, MAX_BATCH_SIZE);
        }
        return toTranslate;
    }

    private List<TranslationResponse> mergeResults(List<TranslationResponse> translations, Map<Integer, TranslationResponse> cachedMap, List<TranslationRequest> batch) {
        return batch.stream()
                .map(req -> Optional.ofNullable(getTranslatedOrCached(translations, cachedMap, req.getAnilistId()))
                        .orElseGet(() -> createErrorResponse(req.getAnilistId(), "Translation failed or skipped")))
                .toList();
    }

    private TranslationResponse getTranslatedOrCached(List<TranslationResponse> translations, Map<Integer, TranslationResponse> cachedMap, Integer anilistId) {
        return translations.stream().filter(t -> t.getAnilistId().equals(anilistId)).findFirst()
                .orElse(cachedMap.get(anilistId));
    }

    private List<TranslationResponse> createErrorResponses(List<TranslationRequest> batch) {
        return batch.stream().map(req -> {
            TranslationResponse resp = new TranslationResponse();
            resp.setAnilistId(req.getAnilistId());
            resp.setError("Batch translation failed");
            return resp;
        }).toList();
    }

    private TranslationResponse createErrorResponse(Integer anilistId, String errorMsg) {
        TranslationResponse resp = new TranslationResponse();
        resp.setAnilistId(anilistId);
        resp.setError(errorMsg);
        return resp;
    }

    private List<TranslationResponse> mapToResponses(List<TranslationRequest> batch, Map<Integer, TranslationResponse> cachedMap) {
        return batch.stream().map(req -> cachedMap.get(req.getAnilistId())).toList();
    }
}