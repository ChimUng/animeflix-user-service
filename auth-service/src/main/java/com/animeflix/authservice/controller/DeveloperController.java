package com.animeflix.authservice.controller;

import com.animeflix.authservice.DTO.LoginDevRequest;
import com.animeflix.authservice.DTO.LoginDevResponse;
import com.animeflix.authservice.DTO.RegisterDevRequest;
import com.animeflix.authservice.DTO.RegisterDevResponse;
import com.animeflix.authservice.service.DeveloperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class DeveloperController {

    private final DeveloperService authService;

    @PostMapping("/dev/register")
    public Mono<ResponseEntity<RegisterDevResponse>> registerDev (@RequestBody RegisterDevRequest req) {
        return authService.registerDeveloper(req)
                .map(resp -> ResponseEntity.status(HttpStatus.CREATED).body(resp));
    }

    @PostMapping("/dev/login")
    public Mono<ResponseEntity<LoginDevResponse>> loginDev(@RequestBody LoginDevRequest req) {
        return authService.loginDeveloper(req)
                .map(ResponseEntity::ok);
    }

    /**
     * API Test Rate Limit
     * - Nếu gọi được API này -> Filter đã cho qua (Key đúng + Còn lượt)
     * - Nếu bị chặn -> Filter trả lỗi 429 hoặc 401
     */
    @GetMapping("/dev/test-limit")
    public Mono<ResponseEntity<String>> testRateLimit(@RequestHeader("X-API-KEY") String apiKey) {
        return authService.validateApiKey(apiKey) // Gọi lại để lấy thông tin (thực tế Filter đã gọi rồi)
                .map(dev -> {
                    return ResponseEntity.ok(
                            "Thành công! Request đã vượt qua Rate Limit.\n" +
                                    "App: " + dev.getAppName() + "\n" +
                                    "Limit: " + dev.getRateLimit() + "/h"
                    );
                });
    }

    /**
     * API For Gateway caliing with authservice ( Validate APIKEY Controller)
     * - Nếu gọi được API này -> Filter đã cho qua (Key đúng + Còn lượt)
     * - Nếu bị chặn -> Filter trả lỗi 429 hoặc 401
     */
    @PostMapping("/internal/validate-key")
    public Mono<ResponseEntity<Void>> validateKeyInternal(@RequestHeader("X-API-KEY") String apiKey) {
        return authService.validateApiKey(apiKey)
                .map(dev -> ResponseEntity.ok().<Void>build())
                .onErrorResume(e -> {
                    if (e.getMessage() != null && e.getMessage().contains("Rate limit")) {
                        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }
}