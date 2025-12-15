package com.animeflix.userservice.controller;

import com.animeflix.userservice.dto.request.UpdatePreferencesRequest;
import com.animeflix.userservice.dto.response.UserPreferenceResponse;
import com.animeflix.userservice.exception.ApiResponse;
import com.animeflix.userservice.service.UserPreferenceService;
import com.animeflix.userservice.util.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/user/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<UserPreferenceResponse>>> getPreferences(
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(preferenceService::getPreferences)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    @PutMapping
    public Mono<ResponseEntity<ApiResponse<UserPreferenceResponse>>> updatePreferences(
            @Valid @RequestBody UpdatePreferencesRequest request,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(userId -> preferenceService.updatePreferences(userId, request))
                .map(response -> ResponseEntity.ok(ApiResponse.success(
                        "Preferences updated successfully", response)));
    }
}