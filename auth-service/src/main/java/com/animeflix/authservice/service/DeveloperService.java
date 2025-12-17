package com.animeflix.authservice.service;

import com.animeflix.authservice.DTO.LoginDevRequest;
import com.animeflix.authservice.DTO.LoginDevResponse;
import com.animeflix.authservice.DTO.RegisterDevRequest;
import com.animeflix.authservice.DTO.RegisterDevResponse;
import com.animeflix.authservice.Entity.Developer;
import com.animeflix.authservice.Repository.DeveloperRepository;
import com.animeflix.authservice.mapper.DeveloperMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeveloperService {

    private final DeveloperRepository devRepo;
    private final PasswordEncoder passwordEncoder;
    private final DeveloperMapper devMapper;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * ĐĂNG KÝ DEVELOPER – TRẢ VỀ API KEY NGAY
     */
    public Mono<RegisterDevResponse> registerDeveloper(RegisterDevRequest req) {
        log.info("Registering developer: appId={}, email={}", req.getAppId(), req.getEmail());

        return devRepo.findByAppId(req.getAppId())
                .flatMap(exists -> Mono.<Developer>error(new RuntimeException("App ID đã tồn tại")))
                .switchIfEmpty(devRepo.findByEmail(req.getEmail())
                        .flatMap(exists -> Mono.<Developer>error(new RuntimeException("Email đã được dùng")))
                        .switchIfEmpty(Mono.defer(() -> {
                            // DÙNG MAPPER TẠO ENTITY GỐC
                            Developer dev = devMapper.toEntity(req);

                            // CHỈ CÒN CÁI NÀY LÀM Ở SERVICE
                            dev.setPasswordHash(passwordEncoder.encode(req.getPassword()));
                            dev.setClientId("cli_" + RandomStringUtils.randomAlphanumeric(14));
                            dev.setClientSecret("sec_" + RandomStringUtils.randomAlphanumeric(32));
                            dev.setApiKey("api_" + RandomStringUtils.randomAlphanumeric(28));
                            dev.setLastUsedAt(null);

                            log.info("Saving new developer: {}", dev.getAppId());
                            return devRepo.save(dev);
                        })))
                .map(devMapper::toRegisterResponse);
    }

    public Mono<LoginDevResponse> loginDeveloper(LoginDevRequest req) {
        return devRepo.findByUsername(req.getUsername())
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid email or password")))
                .flatMap(dev -> {
                    if (!passwordEncoder.matches(req.getPassword(), dev.getPasswordHash())) {
                        return Mono.error(new RuntimeException("Invalid username or password"));
                    }
                    if (!dev.isActive()) {
                        return Mono.error(new RuntimeException("Developer account is disabled"));
                    }
                    dev.setLastUsedAt(LocalDateTime.now());
                    return devRepo.save(dev)
                            .map(devMapper::toLoginResponse);
                });
    }
    /**
     * Hàm kiểm tra Rate Limit với Redis
     */
    private Mono<Boolean> checkRateLimit(String apiKey, int limit) {
        String key = "rate_limit:" + apiKey;
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(key, Duration.ofHours(1))
                                .thenReturn(true);
                    }
                    if (count > limit) {
                        return Mono.just(false);
                    }
                    return Mono.just(true);
                });
    }

    /**
     * Validate Key + Rate Limit
     */
    public Mono<Developer> validateApiKey(String apiKey) {
        return devRepo.findByApiKey(apiKey)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid API Key")))
                .flatMap(dev -> {
                    if (!dev.isActive()) {
                        return Mono.error(new RuntimeException("API Key đã bị khóa"));
                    }
                    return checkRateLimit(apiKey, dev.getRateLimit())
                            .flatMap(isAllowed -> {
                                if (!isAllowed) {
                                    return Mono.error(new RuntimeException("Rate limit exceeded (" + dev.getRateLimit() + " req/hour)"));
                                }
                                dev.setLastUsedAt(LocalDateTime.now());
                                return devRepo.save(dev).thenReturn(dev);
                            });
                });
    }
}