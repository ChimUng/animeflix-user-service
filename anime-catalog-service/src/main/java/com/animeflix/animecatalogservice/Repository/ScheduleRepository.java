package com.animeflix.animecatalogservice.Repository;

import com.animeflix.animecatalogservice.Entity.AnimeSchedule;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ScheduleRepository extends MongoRepository<AnimeSchedule, String> {

    // Query schedule trong time range
    List<AnimeSchedule> findByAiringAtBetweenOrderByAiringAtAsc(Long start, Long end);

    // Query theo ngày
    List<AnimeSchedule> findByDayOrderByAiringAtAsc(String day);

    // Check tồn tại
    boolean existsByAnimeIdAndEpisode(String animeId, Integer episode);

    // Xóa schedule cũ (backup cho TTL)
    void deleteByAiringAtLessThan(Long timestamp);

    // Count theo ngày - Aggregation query
    @Aggregation(pipeline = {
            "{ $match: { airingAt: { $gte: ?0, $lte: ?1 } } }",
            "{ $group: { _id: '$day', count: { $sum: 1 } } }",
            "{ $project: { _id: 0, day: '$_id', count: 1 } }"
    })
    List<DayCountProjection> countByDay(Long start, Long end);

    // Projection interface cho aggregation result
    interface DayCountProjection {
        String getDay();
        Long getCount();
    }
}