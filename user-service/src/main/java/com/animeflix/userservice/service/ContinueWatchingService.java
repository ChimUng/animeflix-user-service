package com.animeflix.userservice.service;

import com.animeflix.userservice.dto.response.ContinueWatchingResponse;
import com.animeflix.userservice.entity.ContinueWatching;
import com.animeflix.userservice.entity.WatchHistory;
import com.animeflix.userservice.mapper.ContinueWatchingMapper;
import com.animeflix.userservice.repository.ContinueWatchingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContinueWatchingService {

    private final ContinueWatchingRepository continueRepo;
    private final ContinueWatchingMapper mapper;

    @Value("${features.continue-watching.max-items:20}")
    private int maxItems;

    /**
     * Lấy danh sách "Xem tiếp"
     */
    public Flux<ContinueWatchingResponse> getContinueWatching(String userId) {
        return continueRepo.findByUserIdOrderByLastWatchedAtDesc(
                        userId, PageRequest.of(0, maxItems))
                .map(mapper::toResponse);
    }

    /**
     * Update từ watch history (called automatically)
     */
    public Mono<ContinueWatching> updateFromHistory(WatchHistory history) {
        // Chỉ update nếu chưa xem xong (progress < 0.9)
        if (history.getCompleted() ||
                (history.getProgress() != null && history.getProgress() >= 0.9)) {
            return removeFromContinueWatching(history.getUserId(), history.getAniId())
                    .then(Mono.empty());
        }

        return continueRepo.findByUserIdAndAniId(history.getUserId(), history.getAniId())
                .flatMap(existing -> updateExisting(existing, history))
                .switchIfEmpty(Mono.defer(() -> createNew(history)))
                .flatMap(continueRepo::save)
                .flatMap(saved -> cleanupOldEntries(history.getUserId()).thenReturn(saved));
    }

    private Mono<ContinueWatching> updateExisting(ContinueWatching existing, WatchHistory history) {
        existing.setEpId(history.getEpId());
        existing.setEpNum(history.getEpNum());
        existing.setEpTitle(history.getEpTitle());

        existing.setNextepId(history.getNextepId());
        existing.setNextepNum(history.getNextepNum());

        existing.setTimeWatched(history.getTimeWatched());
        existing.setDuration(history.getDuration());
        existing.setProgress(history.getProgress());

        if (history.getProvider() != null) {
            existing.setProvider(history.getProvider());
        }
        if (history.getSubtype() != null) {
            existing.setSubtype(history.getSubtype());
        }

        existing.setLastWatchedAt(LocalDateTime.now());
        return Mono.just(existing);
    }

    private Mono<ContinueWatching> createNew(WatchHistory history) {
        return Mono.just(ContinueWatching.builder()
                .userId(history.getUserId())

                .aniId(history.getAniId())
                .aniTitle(history.getAniTitle())
                .image(history.getImage())

                .epId(history.getEpId())
                .epNum(history.getEpNum())
                .epTitle(history.getEpTitle())

                .nextepId(history.getNextepId())
                .nextepNum(history.getNextepNum())

                .timeWatched(history.getTimeWatched())
                .duration(history.getDuration())
                .progress(history.getProgress())

                .provider(history.getProvider())
                .subtype(history.getSubtype())

                .lastWatchedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * Xóa khỏi continue watching
     */
    public Mono<Void> removeFromContinueWatching(String userId, String aniId) {
        return continueRepo.deleteByUserIdAndAniId(userId, aniId);
    }

    /**
     * Cleanup: Giữ tối đa maxItems, xóa cái cũ nhất
     */
    private Mono<Void> cleanupOldEntries(String userId) {
        return continueRepo.countByUserId(userId)
                .flatMap(count -> {
                    if (count <= maxItems) {
                        return Mono.empty();
                    }

                    long toDelete = count - maxItems;
                    return continueRepo.findByUserIdOrderByLastWatchedAtAsc(userId)
                            .take(toDelete)
                            .flatMap(continueRepo::delete)
                            .then();
                });
    }
}