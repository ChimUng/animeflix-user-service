package com.animeflix.authservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtUtil {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long accessExp;
    private final long refreshExp;

    public JwtUtil(
            @Value("${jwt.access-secret}") String accessSecretBase64,
            @Value("${jwt.refresh-secret}") String refreshSecretBase64,
            @Value("${jwt.access-exp}") long accessExp,
            @Value("${jwt.refresh-exp}") long refreshExp) {

        this.accessKey = decodeKey(accessSecretBase64);
        this.refreshKey = decodeKey(refreshSecretBase64);
        this.accessExp = accessExp;
        this.refreshExp = refreshExp;
    }

    private SecretKey decodeKey(String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key.getBytes(StandardCharsets.UTF_8));
        if (decoded.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 256 bits!");
        }
        return Keys.hmacShaKeyFor(decoded);
    }

    // ===================== GENERATE =====================
    public String generateAccessToken(String userId, String username, String email) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())                    // JTI
                .subject(userId)
                .claim("username", username)
                .claim("email", email)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExp))
                .signWith(accessKey)                                 // TỰ ĐỘNG HS256
                .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExp))
                .signWith(refreshKey)
                .compact();
    }

    // ===================== PARSE & VALIDATE =====================
    private Claims parseClaims(String token, SecretKey key) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("Invalid token: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public String extractUserId(String token, boolean isRefresh) {
        SecretKey key = isRefresh ? refreshKey : accessKey;
        return parseClaims(token, key).getSubject();
    }

    public String getJti(String token, boolean isRefresh) {
        SecretKey key = isRefresh ? refreshKey : accessKey;
        return parseClaims(token, key).getId();
    }

    public boolean isValid(String token, boolean isRefresh) {
        try {
            SecretKey key = isRefresh ? refreshKey : accessKey;
            parseClaims(token, key);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}