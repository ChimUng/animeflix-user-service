package com.animeflix.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Qualifier;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final WebClient authServiceWebClient;

    public AuthenticationFilter(@Qualifier("authServiceWebClient") WebClient authServiceWebClient) {
        super(Config.class);
        this.authServiceWebClient = authServiceWebClient;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().toString();

            log.info("[Gateway] Request: {} {}", request.getMethod(), path);

            if (path.startsWith("/api/auth/") || path.contains("/actuator")) {
                log.info("[Gateway] Skip auth filter for: {}", path);
                return chain.filter(exchange);
            }

            String apiKey = request.getHeaders().getFirst("X-API-KEY");
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("[Gateway] Missing X-API-KEY for {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            return authServiceWebClient.post()
                    .uri("/api/auth/internal/validate-key")
                    .header("X-API-KEY", apiKey)
                    .retrieve()
                    .toBodilessEntity()
                    .flatMap(response -> {
                        log.info("[Gateway] API Key validated successfully for {}", path);
                        return chain.filter(exchange);
                    })
                    .onErrorResume(e -> {
                        log.error("[Gateway] API Key validation failed: {}", e.getMessage());
                        HttpStatus status = HttpStatus.UNAUTHORIZED;
                        if (e.getMessage() != null && e.getMessage().contains("Rate limit")) {
                            status = HttpStatus.TOO_MANY_REQUESTS;
                        }
                        exchange.getResponse().setStatusCode(status);
                        return exchange.getResponse().setComplete();
                    });
        };
    }

    public static class Config {}
}