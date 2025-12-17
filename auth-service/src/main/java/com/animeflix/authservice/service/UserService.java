package com.animeflix.authservice.service;

import com.animeflix.authservice.DTO.LoginRequest;
import com.animeflix.authservice.DTO.LoginResponse;
import com.animeflix.authservice.DTO.SignupRequest;
import com.animeflix.authservice.DTO.SignupResponse;
import com.animeflix.authservice.Entity.User;
import com.animeflix.authservice.Repository.UserRepository;
import com.animeflix.authservice.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserMapper userMapper;

    public Mono<SignupResponse> signup(SignupRequest req) {
        return userRepo.findByEmail(req.getEmail())
                .flatMap(u -> Mono.<User>error(new RuntimeException("Email exists")))
                .switchIfEmpty(Mono.defer(() -> {

                    User user = userMapper.toEntity(req);

                    user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
                    String encodedEmail = URLEncoder.encode(req.getEmail(), StandardCharsets.UTF_8);
                    user.setAvatar("https://i.pravatar.cc/150?u=" + encodedEmail);

                    return userRepo.save(user);
                }))
                .map(savedUser -> userMapper.toResponse(savedUser));
    }

    public Mono<LoginResponse> login(LoginRequest req, String device, String ip) {
        return userRepo.findByEmail(req.getEmail())
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid email or password")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
                        return Mono.error(new RuntimeException("Invalid email or password"));
                    }
                    return tokenService.createSession(user, device, ip);
                });
    }

    public Mono<Void> logout(String refreshToken) {
        return tokenService.revokeRefreshToken(refreshToken);
    }

    // THÊM METHOD CHO FILTER
    public Mono<User> findById(String id) {
        return userRepo.findById(id);
    }

    public Collection<? extends GrantedAuthority> getAuthorities(User user) {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
