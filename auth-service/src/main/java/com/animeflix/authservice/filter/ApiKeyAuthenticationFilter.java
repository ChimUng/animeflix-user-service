package com.animeflix.authservice.filter;

import com.animeflix.authservice.service.DeveloperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class    ApiKeyAuthenticationFilter implements WebFilter {

    private final DeveloperService developerService;

    public ApiKeyAuthenticationFilter(DeveloperService developerService) {
        this.developerService = developerService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        // Bỏ qua /api/auth/** (register, login, etc.)
        if (path.startsWith("/api/auth/internal/") ||
                path.startsWith("/api/auth/dev/register") ||
                path.startsWith("/api/auth/dev/login") ||
                path.contains("/actuator")) {
            return chain.filter(exchange);
        }

        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Missing X-API-KEY for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse()
                    .writeWith(Mono.just(exchange.getResponse()
                            .bufferFactory()
                            .wrap("Missing API Key".getBytes())));
        }

        return developerService.validateApiKey(apiKey)
                .flatMap(dev -> {
                    log.info("API Key valid: {} (appId: {}, email: {}) → {}",
                            apiKey, dev.getAppId(), dev.getEmail(), path);
                    return chain.filter(exchange);
                })
                .onErrorResume(err -> {
                    HttpStatus status = HttpStatus.UNAUTHORIZED;

                    // Nếu lỗi chứa chữ "Rate limit" -> Đổi thành 429
                    if (err.getMessage().contains("Rate limit")) {
                        status = HttpStatus.TOO_MANY_REQUESTS;
                    }

                    exchange.getResponse().setStatusCode(status);
                    return exchange.getResponse()
                            .writeWith(Mono.just(exchange.getResponse()
                                    .bufferFactory()
                                    .wrap(err.getMessage().getBytes())));
                });
    }
}