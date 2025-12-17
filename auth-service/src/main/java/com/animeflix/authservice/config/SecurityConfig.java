package com.animeflix.authservice.config;

import com.animeflix.authservice.filter.ApiKeyAuthenticationFilter;
import com.animeflix.authservice.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ServerSecurityContextRepository securityContextRepository() {
        return NoOpServerSecurityContextRepository.getInstance();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ServerSecurityContextRepository securityContextRepository,
                                                         JwtAuthenticationFilter jwtAuthFilter,
                                                         ApiKeyAuthenticationFilter apiKeyFilter) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .securityContextRepository(securityContextRepository)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(
                                "/api/auth/user/signup",
                                "/api/auth/user/login",
                                "/api/auth/user/refresh",
                                "/api/auth/user/logout",
                                "/api/auth/dev/register",
                                "/api/auth/dev/login",
                                "/api/auth/internal/**"
                        ).permitAll()
                        .anyExchange().permitAll()
                )
                .addFilterBefore(apiKeyFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterBefore(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable);
        return http.build();
    }
    private static class NoOpSecurityContextRepository implements ServerSecurityContextRepository {
        @Override
        public Mono<Void> save(ServerWebExchange exchange, org.springframework.security.core.context.SecurityContext context) {
            return Mono.empty();
        }
        @Override
        public Mono<org.springframework.security.core.context.SecurityContext> load(ServerWebExchange exchange) {
            return Mono.empty();
        }
    }
}