package com.animeflix.userservice.util;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityContextUtil {

    /**
     * Lấy userId từ header X-User-Id (được Gateway set sau khi validate JWT)
     */
    public static Mono<String> getCurrentUserId(ServerWebExchange exchange) {
        return Mono.justOrEmpty(
                exchange.getRequest().getHeaders().getFirst("X-User-Id")
        ).switchIfEmpty(
                Mono.error(new RuntimeException("User ID not found in request"))
        );
    }
}