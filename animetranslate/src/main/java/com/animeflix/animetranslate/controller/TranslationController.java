package com.animeflix.animetranslate.controller;

import com.animeflix.animetranslate.model.TranslationRequest;
import com.animeflix.animetranslate.model.TranslationResponse;
import com.animeflix.animetranslate.service.TranslationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/animetranslate")
public class TranslationController {

    private final TranslationService service;

    public TranslationController(TranslationService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> translateBatch(@RequestBody Map<String, List<TranslationRequest>> request) {
        List<TranslationRequest> batch = request.get("batch");
        if (batch == null || batch.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "Batch dữ liệu không hợp lệ")));
        }

        return service.translateBatch(batch)
                .map(translated -> ResponseEntity.ok(Map.of(
                        "cached", translated.size() == batch.size() && translated.stream().noneMatch(t -> t.getError() != null),
                        "translated", translated
                )))
                .onErrorReturn(ResponseEntity.internalServerError().body(Map.of("error", "Translate failed")));
    }
}