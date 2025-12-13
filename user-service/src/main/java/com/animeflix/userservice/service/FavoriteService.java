package com.animeflix.userservice.service;

import com.animeflix.userservice.dto.request.AddFavoriteRequest;
import com.animeflix.userservice.dto.response.FavoriteResponse;
import com.animeflix.userservice.entity.Favorite;
import com.animeflix.userservice.exception.DuplicateResourceException;
import com.animeflix.userservice.exception.ResourceNotFoundException;
import com.animeflix.userservice.mapper.FavoriteMapper;
import com.animeflix.userservice.repository.FavoriteRepository;
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
public class FavoriteService {

    private final FavoriteRepository favoriteRepo;
    private final FavoriteMapper mapper;
    private final ExternalAnimeService animeService;

    /**
     * Thêm vào yêu thích
     */
    public Mono<FavoriteResponse> addFavorite(String userId, AddFavoriteRequest request) {
        log.debug("Adding favorite for user: {}, anime: {}", userId, request.getAnimeId());

        return favoriteRepo.existsByUserIdAndAnimeId(userId, request.getAnimeId())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new DuplicateResourceException(
                                "Anime already in favorites"));
                    }
                    return createFavorite(userId, request);
                })
                .flatMap(favoriteRepo::save)
                .map(mapper::toResponse);
    }

    private Mono<Favorite> createFavorite(String userId, AddFavoriteRequest request) {
        // Nếu request có sẵn anime info thì dùng, không thì fetch từ catalog-service
        if (request.getAnimeTitle() != null) {
            return Mono.just(buildFavorite(userId, request));
        }

        return animeService.getAnimeDetails(request.getAnimeId())
                .map(animeDetails -> {
                    request.setAnimeTitle(animeDetails.getTitle());
                    request.setCoverImage(animeDetails.getCoverImage());
                    request.setBannerImage(animeDetails.getBannerImage());
                    request.setStatus(animeDetails.getStatus());
                    request.setTotalEpisodes(animeDetails.getTotalEpisodes());
                    return buildFavorite(userId, request);
                })
                .onErrorResume(e -> {
                    log.warn("Failed to fetch anime details, creating favorite without denormalized data");
                    return Mono.just(buildFavorite(userId, request));
                });
    }

    private Favorite buildFavorite(String userId, AddFavoriteRequest request) {
        return Favorite.builder()
                .userId(userId)
                .animeId(request.getAnimeId())
                .notifyNewEpisode(request.getNotifyNewEpisode())
                .animeTitle(request.getAnimeTitle())
                .coverImage(request.getCoverImage())
                .bannerImage(request.getBannerImage())
                .status(request.getStatus())
                .totalEpisodes(request.getTotalEpisodes())
                .addedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Lấy danh sách yêu thích (phân trang)
     */
    public Flux<FavoriteResponse> getFavorites(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return favoriteRepo.findByUserIdOrderByAddedAtDesc(userId, pageable)
                .map(mapper::toResponse);
    }

    /**
     * Lấy tất cả yêu thích (không phân trang)
     */
    public Flux<FavoriteResponse> getAllFavorites(String userId) {
        return favoriteRepo.findByUserIdOrderByAddedAtDesc(userId)
                .map(mapper::toResponse);
    }

    /**
     * Kiểm tra đã yêu thích chưa
     */
    public Mono<Boolean> isFavorite(String userId, String animeId) {
        return favoriteRepo.existsByUserIdAndAnimeId(userId, animeId);
    }

    /**
     * Xóa khỏi yêu thích
     */
    public Mono<Void> removeFavorite(String userId, String animeId) {
        return favoriteRepo.existsByUserIdAndAnimeId(userId, animeId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ResourceNotFoundException(
                                "Anime not found in favorites"));
                    }
                    return favoriteRepo.deleteByUserIdAndAnimeId(userId, animeId);
                });
    }

    /**
     * Toggle notification cho anime yêu thích
     */
    public Mono<FavoriteResponse> toggleNotification(String userId, String animeId) {
        return favoriteRepo.findByUserIdAndAnimeId(userId, animeId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Anime not found in favorites")))
                .flatMap(favorite -> {
                    favorite.setNotifyNewEpisode(!favorite.getNotifyNewEpisode());
                    favorite.setUpdatedAt(LocalDateTime.now());
                    return favoriteRepo.save(favorite);
                })
                .map(mapper::toResponse);
    }

    /**
     * Đếm số anime yêu thích
     */
    public Mono<Long> countFavorites(String userId) {
        return favoriteRepo.countByUserId(userId);
    }

    /**
     * Lấy danh sách favorites có bật thông báo (cho scheduler)
     */
    public Flux<Favorite> getFavoritesWithNotification() {
        return favoriteRepo.findByNotifyNewEpisodeTrue();
    }

    /**
     * Lấy favorites có bật thông báo của user
     */
    public Flux<Favorite> getUserFavoritesWithNotification(String userId) {
        return favoriteRepo.findByUserIdAndNotifyNewEpisodeTrue(userId);
    }
}