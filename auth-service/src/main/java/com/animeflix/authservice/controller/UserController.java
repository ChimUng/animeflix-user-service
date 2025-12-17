package com.animeflix.authservice.controller;

import com.animeflix.authservice.DTO.*;
import com.animeflix.authservice.Entity.User;
import com.animeflix.authservice.mapper.UserMapper;
import com.animeflix.authservice.service.TokenService;
import com.animeflix.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;
    private final UserMapper userMapper;

    /**
     * API lấy thông tin người dùng hiện tại từ Token/Cookie.
     * SecurityConfig đã chặn /api/auth/user/** nên API này bắt buộc phải authen mới vào được.
     */
    @GetMapping("/me")
    public Mono<ProfileResponse> getCurrentUser(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Mono.just(user)
                .map(userMapper::toProfileResponse);
    }

    @PostMapping("/signup")
    public Mono<SignupResponse> signup(@RequestBody SignupRequest req) {
        return userService.signup(req);
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(
            @RequestBody LoginRequest req,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        String ip = request.getRemoteAddress().getHostString();
        String device = request.getHeaders().getFirst("User-Agent");

        return userService.login(req, device, ip)
                .map(loginResp -> {
                    setAuthCookies(response, loginResp.getAccessToken(), loginResp.getRefreshToken());
                    // Không trả token trong body nữa (tránh lộ)
                    return ResponseEntity.ok(LoginResponse.builder()
                            .userId(loginResp.getUserId())
                            .username(loginResp.getUsername())
                            .email(loginResp.getEmail())
                            .expiresAt(loginResp.getExpiresAt())
                            .build());
                });
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<LoginResponse>> refresh(ServerHttpRequest request, ServerHttpResponse response) {
        return Mono.justOrEmpty(request.getCookies().getFirst("refresh_token"))
                .switchIfEmpty(Mono.error(new RuntimeException("Missing refresh token")))
                .map(cookie -> cookie.getValue())
                .flatMap(tokenService::refresh)
                .map(newTokens -> {
                    setAuthCookies(response, newTokens.getAccessToken(), newTokens.getRefreshToken());
                    return ResponseEntity.ok(LoginResponse.builder()
                            .userId(newTokens.getUserId())
                            .username(newTokens.getUsername())
                            .email(newTokens.getEmail())
                            .expiresAt(newTokens.getExpiresAt())
                            .build());
                });
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(ServerHttpRequest request, ServerHttpResponse response) {
        return Mono.justOrEmpty(request.getCookies().getFirst("refresh_token"))
                .map(cookie -> cookie.getValue())
                .flatMap(userService::logout)
                .then(Mono.fromRunnable(() -> {
                    // Xóa cả 2 cookie
                    response.addCookie(ResponseCookie.from("access_token", "")
                            .maxAge(0).path("/").domain(".animeflix.com").build());
                    response.addCookie(ResponseCookie.from("refresh_token", "")
                            .maxAge(0).path("/api/auth/user/refresh").domain(".animeflix.com").build());
                }))
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    private void setAuthCookies(ServerHttpResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(false)           // JS phải đọc được để gọi Bearer
                .secure(false)
                .sameSite("Lax")
//                .domain(".animeflix.com")  // quan trọng: chia sẻ giữa animeflix.com và api.animeflix.com
                .path("/")
                .maxAge(3600)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)            // chống XSS
                .secure(false)
                .sameSite("Lax")
//                .domain(".animeflix.com")
                .path("/api/auth/user/refresh")
                .maxAge(30 * 24 * 60 * 60L)
                .build();

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }
}