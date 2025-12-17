package com.animeflix.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UnauthorizedEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, org.springframework.security.core.AuthenticationException e) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"error\":\"Unauthorized\",\"message\":\"Bạn chưa đăng nhập\"}";

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
    }
}