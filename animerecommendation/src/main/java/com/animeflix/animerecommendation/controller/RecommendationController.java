package com.animeflix.animerecommendation.controller;

import com.animeflix.animerecommendation.model.RecommendationResponse;
import com.animeflix.animerecommendation.service.AuthService;
import com.animeflix.animerecommendation.service.RecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> getRecommendations() {
        String userName = authService.getCurrentUserName();
        if (userName == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\": \"Unauthorized\"}"));
        }

        return recommendationService.getRecommendations(userName)
                .map(recommendations -> {
                    try {
                        RecommendationResponse response = new RecommendationResponse(recommendations);
                        String json = objectMapper.writeValueAsString(response);
                        return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(json);
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("{\"message\": \"Internal Server Error\"}");
                    }
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"No watch history\"}")))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Internal Server Error\"}"));
    }
}