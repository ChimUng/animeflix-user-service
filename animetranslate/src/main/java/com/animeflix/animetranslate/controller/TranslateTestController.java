package com.animeflix.animetranslate.controller;

import com.animeflix.animetranslate.model.TranslationRequest;
import com.animeflix.animetranslate.model.TranslationResponse;
import com.animeflix.animetranslate.service.GeminiClient;
import com.animeflix.animetranslate.service.PromptBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/testtranslate")
public class TranslateTestController {

    private final GeminiClient geminiClient;
    private final PromptBuilder promptBuilder;

    public TranslateTestController(GeminiClient geminiClient, PromptBuilder promptBuilder) {
        this.geminiClient = geminiClient;
        this.promptBuilder = promptBuilder;
    }

    @GetMapping
    public Mono<List<TranslationResponse>> testTranslate() {
        List<TranslationRequest> mockRequests = List.of(
                new TranslationRequest(1, "One Piece", "The story of a boy who wants to become the Pirate King.")
        );
        List<Map<String, String>> prompts = promptBuilder.build(mockRequests);
        return geminiClient.translate(prompts);
    }
}
