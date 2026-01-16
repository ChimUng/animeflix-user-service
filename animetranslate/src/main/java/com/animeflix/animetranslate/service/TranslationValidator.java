package com.animeflix.animetranslate.service;

import com.animeflix.animetranslate.exception.ValidationException;
import com.animeflix.animetranslate.model.TranslationResponse;
import com.animeflix.animetranslate.repository.TranslationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class TranslationValidator {

    private static final Logger log = LoggerFactory.getLogger(TranslationValidator.class);
    private static final Pattern EMPTY_DESC_REGEX = Pattern.compile("^(?i)\\$\\$ ?(Chưa có|Không có|Hiện chưa có) \\s* mô tả \\.? \\$\\$? $");

    private final TranslationRepository repository;

    public TranslationValidator(TranslationRepository repository) {
        this.repository = repository;
    }

    public List<TranslationResponse> filterValid(List<TranslationResponse> translations) {
        List<TranslationResponse> valid = translations.stream()
                .filter(this::isValidTranslation)
                .toList();

        if (valid.isEmpty()) {
            log.info("No valid translations to save.");
        } else {
            log.info("Found {} valid translations.", valid.size());
        }
        return valid;
    }

    public Mono<Void> upsertIfValid(List<TranslationResponse> validTranslations) {
        if (validTranslations.isEmpty()) {
            return Mono.empty();
        }
        log.info("Upserting {} translations to DB...", validTranslations.size());
        return repository.upsertTranslations(validTranslations)
                .doOnSuccess(v -> log.info("Upsert completed successfully."))
                .doOnError(e -> {
                    log.error("Upsert failed", e);
                    throw new ValidationException("Failed to upsert translations: " + e.getMessage(), e);
                });
    }

    private boolean isValidTranslation(TranslationResponse t) {
        if (t.getError() != null) {
            return false;
        }
        String desc = t.getDescriptionVi();
        if (desc == null || desc.trim().isEmpty() || "No description available".equals(desc)) {
            return false;
        }
        if (EMPTY_DESC_REGEX.matcher(desc.trim()).matches()) {
            log.debug("Skipping empty desc for ID: {}", t.getAnilistId());
            return false;
        }
        return true;
    }
}
