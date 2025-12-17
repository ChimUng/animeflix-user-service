package com.animeflix.authservice.service;

import com.animeflix.authservice.DTO.LoginResponse;
import com.animeflix.authservice.Entity.Session;
import com.animeflix.authservice.Entity.User;
import com.animeflix.authservice.Repository.SessionRepository;
import com.animeflix.authservice.Repository.UserRepository;
import com.animeflix.authservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtil jwtUtil;
    private final SessionRepository sessionRepo;
    private final UserRepository userRepo;

    public Mono<LoginResponse> createSession(User user, String device, String ip) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        Session session = new Session();
        session.setUserId(user.getId());
        session.setAccessToken(accessToken);
        session.setRefreshToken(refreshToken);
        session.setExpiresAt(LocalDateTime.now().plusDays(30));
        session.setDevice(device);
        session.setIp(ip);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastUsedAt(LocalDateTime.now());

        return sessionRepo.save(session)
                .map(s -> LoginResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .expiresIn(3600)
                        .userId(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .expiresAt(s.getExpiresAt())
                        .build());
    }

    public Mono<LoginResponse> refresh(String refreshToken) {
        return sessionRepo.findByRefreshToken(refreshToken)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid refresh token")))
                .flatMap(session -> {
                    if (session.isRevoked()) {
                        return Mono.error(new RuntimeException("Token revoked"));
                    }
                    if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return Mono.error(new RuntimeException("Token expired"));
                    }

                    // KIỂM TRA REFRESH TOKEN HỢP LỆ BẰNG JwtUtil MỚI
                    if (!jwtUtil.isValid(refreshToken, true)) {
                        return Mono.error(new RuntimeException("Invalid refresh token signature"));
                    }

                    session.setLastUsedAt(LocalDateTime.now());
                    return sessionRepo.save(session)
                            .then(userRepo.findById(session.getUserId()))
                            .map(user -> LoginResponse.builder()
                                    .accessToken(jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getEmail()))
                                    .refreshToken(refreshToken)
                                    .expiresIn(3600)
                                    .userId(user.getId())
                                    .username(user.getUsername())
                                    .email(user.getEmail())
                                    .expiresAt(session.getExpiresAt())
                                    .build());
                });
    }

    public Mono<Void> revokeRefreshToken(String refreshToken) {
        return sessionRepo.findByRefreshToken(refreshToken)
                .doOnNext(s -> s.setRevoked(true))
                .flatMap(sessionRepo::save)
                .then();
    }
}