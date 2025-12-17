//package com.animeflix.apigateway.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.web.server.SecurityWebFilterChain;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.reactive.CorsConfigurationSource;
//import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
//
//import java.util.List;
//
//@Configuration
//public class SecurityConfig {
//
//    @Bean
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//        http
//                // Tắt CSRF – vì đây là API Gateway, không dùng form login
//                .csrf(ServerHttpSecurity.CsrfSpec::disable)
//
//                // Bật CORS toàn cục
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//
//                // Tắt mấy cái không dùng
//                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
//                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
//
//                // Cho phép tất cả request đi qua (vì auth đã xử lý ở Filter riêng)
//                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
//
//        return http.build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//
//        // Cho phép các origin bạn sẽ dùng khi dev + production
//        config.setAllowedOriginPatterns(List.of(
//                "http://localhost:3000",   // React dev
//                "http://localhost:5173",   // Vite
//                "http://localhost:8080",   // nếu test trực tiếp
//                "https://animeflix.vercel.app",     // domain thật của bạn sau này
//                "https://your-production-domain.com"
//        ));
//
//        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        config.setAllowedHeaders(List.of("*"));
//        config.setAllowCredentials(true); // quan trọng nếu dùng cookie/auth
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }
//}
