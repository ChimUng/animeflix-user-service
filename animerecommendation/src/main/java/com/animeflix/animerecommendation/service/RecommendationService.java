package com.animeflix.animerecommendation.service;

import com.animeflix.animerecommendation.model.AnimeMeta;
import com.animeflix.animerecommendation.model.RecentEpisode;
import com.animeflix.animerecommendation.model.ScoredRecentEpisode;
import com.animeflix.animerecommendation.model.Watch;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private WatchHistoryService watchHistoryService;

    @Autowired
    private AniListService aniListService;

    public Mono<List<ScoredRecentEpisode>> getRecommendations(String userName) {
        System.out.println(">>> [DEBUG] Bắt đầu lấy gợi ý cho user: " + userName);
        List<Watch> history = watchHistoryService.getWatchHistory(userName);
        System.out.println(">>> [DEBUG] Lịch sử xem lấy được: " + (history != null ? history.size() : 0) + " mục");
        if (history.isEmpty()) {
            return Mono.empty();
        }

        List<Watch> recentHistory = history.subList(0, Math.min(5, history.size()));
        System.out.println(">>> [DEBUG] Lấy " + recentHistory.size() + " anime gần nhất để tính toán gợi ý");

        Flux<AnimeMeta> metasFlux = Flux.fromIterable(recentHistory)
                .map(Watch::getAniId)
                .flatMap(aniListService::fetchAnimeMeta)
                .filter(meta -> meta != null && meta.getGenres() != null)
                .doOnNext(meta -> System.out.println(">>> [DEBUG] Meta lấy được: " + meta.getTitle() + " | Genres: " + meta.getGenres()));

        Mono<Map<String, Integer>> genreCountMono = metasFlux
                .flatMap(meta -> Flux.fromIterable(meta.getGenres()))
                .collectMultimap(genre -> genre, genre -> 1)
                .map(multi -> multi.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())))
                .doOnNext(map -> System.out.println(">>> [DEBUG] Thống kê thể loại: " + map));

        Mono<List<RecentEpisode>> recentMono = aniListService.fetchRecentFromAnilist()
            .doOnNext(list -> System.out.println(">>> [DEBUG] Tổng số anime recent lấy từ AniList: " + list.size()));

        return Mono.zip(genreCountMono, recentMono)
                .map(tuple -> {
                    Map<String, Integer> genreCount = tuple.getT1();
                    List<RecentEpisode> recent = tuple.getT2();

                    List<ScoredRecentEpisode> result = recent.stream()
                            .map(anime -> {
                                ScoredRecentEpisode scored = new ScoredRecentEpisode();
                                BeanUtils.copyProperties(anime, scored);
                                int score = 0;
                                if (anime.getGenres() != null) {
                                    for (String g : anime.getGenres()) {
                                        score += genreCount.getOrDefault(g, 0);
                                    }
                                }
                                scored.setScore(score);
                                return scored;
                            })
                            .filter(s -> s.getScore() > 0)
                            .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                            .limit(10)
                            .collect(Collectors.toList());

                    System.out.println(">>> [DEBUG] Số lượng gợi ý cuối cùng: " + result.size());
                    return result;
                })
                .doOnError(e -> System.err.println(">>> [ERROR] RecommendationService lỗi: " + e.getMessage()));
    }
}