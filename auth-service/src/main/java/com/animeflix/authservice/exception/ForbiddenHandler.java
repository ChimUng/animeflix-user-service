package com.animeflix.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ForbiddenHandler implements ServerAccessDeniedHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, org.springframework.security.access.AccessDeniedException denied) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"error\":\"Forbidden\",\"message\":\"Bạn không có quyền truy cập tài nguyên này\"}";

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
    }
}
