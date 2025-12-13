package com.animeflix.userservice.service;

import com.animeflix.userservice.dto.request.UpdatePreferencesRequest;
import com.animeflix.userservice.dto.response.UserPreferenceResponse;
import com.animeflix.userservice.entity.UserPreference;
import com.animeflix.userservice.mapper.UserPreferenceMapper;
import com.animeflix.userservice.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceService {

    private final UserPreferenceRepository preferenceRepo;
    private final UserPreferenceMapper mapper;

    /**
     * Lấy preferences của user (tạo mới nếu chưa có)
     */
    public Mono<UserPreferenceResponse> getPreferences(String userId) {
        return preferenceRepo.findByUserId(userId)
                .switchIfEmpty(createDefaultPreferences(userId))
                .map(mapper::toResponse);
    }

    /**
     * Update preferences
     */
    public Mono<UserPreferenceResponse> updatePreferences(String userId, UpdatePreferencesRequest request) {
        return preferenceRepo.findByUserId(userId)
                .switchIfEmpty(createDefaultPreferences(userId))
                .flatMap(preference -> {
                    mapper.updateEntity(request, preference);
                    preference.setUpdatedAt(LocalDateTime.now());
                    return preferenceRepo.save(preference);
                })
                .map(mapper::toResponse);
    }

    /**
     * Tạo default preferences
     */
    private Mono<UserPreference> createDefaultPreferences(String userId) {
        log.info("Creating default preferences for user: {}", userId);

        UserPreference defaultPrefs = UserPreference.builder()
                .userId(userId)
                .enableNotifications(true)
                .notifyOnlyFavorites(false)
                .notifyRecommendations(true)
                .autoPlayNext(true)
                .preferredQuality("1080p")
                .preferredLanguage("sub")
                .publicProfile(false)
                .showWatchHistory(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return preferenceRepo.save(defaultPrefs);
    }

    /**
     * Check xem user có bật notifications không
     */
    public Mono<Boolean> isNotificationEnabled(String userId) {
        return preferenceRepo.findByUserId(userId)
                .map(UserPreference::getEnableNotifications)
                .defaultIfEmpty(true);
    }
}
