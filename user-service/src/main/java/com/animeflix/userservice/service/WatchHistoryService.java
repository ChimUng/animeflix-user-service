package com.animeflix.userservice.service;

import com.animeflix.userservice.dto.request.AddHistoryRequest;
import com.animeflix.userservice.dto.response.WatchHistoryResponse;
import com.animeflix.userservice.entity.WatchHistory;
import com.animeflix.userservice.mapper.WatchHistoryMapper;
import com.animeflix.userservice.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchHistoryService {

    private final WatchHistoryRepository historyRepo;
    private final WatchHistoryMapper mapper;
    private final ContinueWatchingService continueWatchingService;

    /**
     * Thêm/Update watch history
     */
    public Mono<WatchHistoryResponse> addOrUpdateHistory(String userId, AddHistoryRequest request) {
        log.debug("Adding watch history for user: {}, anime: {}, episode: {}",
                userId, request.getAniId(), request.getEpNum());

        // Tìm history của episode này
        return historyRepo.findByUserIdAndAniIdAndEpId(userId, request.getAniId(), request.getEpId())
                .flatMap(existing -> updateExisting(existing, request))
                .switchIfEmpty(Mono.defer(() -> createNew(userId, request)))
                .flatMap(historyRepo::save)
                .doOnNext(saved -> {
                    // Async update continue-watching
                    continueWatchingService.updateFromHistory(saved)
                            .subscribe(
                                    cw -> log.debug("Updated continue-watching for anime: {}", saved.getAniId()),
                                    err -> log.warn("Failed to update continue-watching: {}", err.getMessage())
                            );
                })
                .map(mapper::toResponse);
    }

    private Mono<WatchHistory> updateExisting(WatchHistory existing, AddHistoryRequest request) {
        // Update progress
        existing.setTimeWatched(request.getTimeWatched());
        existing.setDuration(request.getDuration());
        existing.setProgress(calculateProgress(request.getTimeWatched(), request.getDuration()));
        existing.setCompleted(request.getCompleted());

        // Update episode info (nếu có thay đổi)
        if (request.getEpTitle() != null) {
            existing.setEpTitle(request.getEpTitle());
        }

        // Update next episode info
        existing.setNextepId(request.getNextepId());
        existing.setNextepNum(request.getNextepNum());

        // Update settings
        if (request.getProvider() != null) {
            existing.setProvider(request.getProvider());
        }
        if (request.getSubtype() != null) {
            existing.setSubtype(request.getSubtype());
        }
        if (request.getDevice() != null) {
            existing.setDevice(request.getDevice());
        }
        if (request.getQuality() != null) {
            existing.setQuality(request.getQuality());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return Mono.just(existing);
    }

    private Mono<WatchHistory> createNew(String userId, AddHistoryRequest request) {
        return Mono.just(WatchHistory.builder()
                .userId(userId)
                // Anime info
                .aniId(request.getAniId())
                .aniTitle(request.getAniTitle())
                .image(request.getImage())
                // Episode info
                .epId(request.getEpId())
                .epNum(request.getEpNum())
                .epTitle(request.getEpTitle())
                // Progress
                .timeWatched(request.getTimeWatched())
                .duration(request.getDuration())
                .progress(calculateProgress(request.getTimeWatched(), request.getDuration()))
                .completed(request.getCompleted())
                // Next episode
                .nextepId(request.getNextepId())
                .nextepNum(request.getNextepNum())
                // Provider & settings
                .provider(request.getProvider())
                .subtype(request.getSubtype() != null ? request.getSubtype() : "sub")
                // Device
                .device(request.getDevice())
                .quality(request.getQuality())
                // Timestamps
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    /**
     * Calculate progress (0.0 - 1.0)
     */
    private Double calculateProgress(Double timeWatched, Double duration) {
        if (timeWatched == null || duration == null || duration == 0) {
            return 0.0;
        }
        return Math.min(1.0, timeWatched / duration);
    }

    /**
     * Lấy lịch sử xem (phân trang)
     */
    public Flux<WatchHistoryResponse> getHistory(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return historyRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(mapper::toResponse);
    }

    /**
     * Lấy lịch sử xem của 1 anime cụ thể
     */
    public Flux<WatchHistoryResponse> getHistoryByAnime(String userId, String aniId) {
        return historyRepo.findByUserIdAndAniIdOrderByCreatedAtDesc(userId, aniId)
                .map(mapper::toResponse);
    }

    /**
     * Xóa lịch sử của 1 anime
     */
    public Mono<Void> deleteByAnime(String userId, String aniId) {
        return historyRepo.deleteByUserIdAndAniId(userId, aniId);
    }

    /**
     * Xóa toàn bộ lịch sử
     */
    public Mono<Void> clearHistory(String userId) {
        return historyRepo.deleteByUserId(userId);
    }

    /**
     * Đếm số anime đã xem
     */
    public Mono<Long> countAnimeWatched(String userId) {
        return historyRepo.countDistinctAnimeByUserId(userId);
    }

    /**
     * Tổng thời gian xem
     */
    public Mono<Long> getTotalWatchedSeconds(String userId) {
        return historyRepo.getTotalWatchedSeconds(userId);
    }
}