package com.animeflix.aisearchservice.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * JSON mà Gemini trả về sau khi parse user query.
 * Tất cả giá trị phải nằm trong enum cho phép (ràng buộc bằng prompt).
 *
 * Ví dụ query "anime hài hước học đường đang chiếu":
 * {
 *   "genres": ["COMEDY", "SCHOOL"],
 *   "status": "RELEASING",
 *   "format": "TV",
 *   "sort": ["TRENDING_DESC"],
 *   "confidence": 0.92,
 *   "fallbackToEmbedding": false,
 *   "reasoning": "Hài hước → COMEDY, Học đường → SCHOOL, Đang chiếu → RELEASING"
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedQueryDTO {

    // Chỉ các genre hợp lệ theo AniList API
    private List<String> genres;

    // Chỉ các tag hợp lệ theo AniList
    private List<String> tags;

    // TV | MOVIE | OVA | ONA | SPECIAL | MUSIC
    private String format;

    // RELEASING | FINISHED | NOT_YET_RELEASED | CANCELLED | HIATUS
    private String status;

    // WINTER | SPRING | SUMMER | FALL
    private String season;

    private Integer seasonYear;

    // POPULARITY_DESC | SCORE_DESC | TRENDING_DESC | FAVOURITES_DESC
    private List<String> sort;

    // [0.0 - 1.0] - LLM tự đánh giá độ tự tin khi map sang genre
    private Double confidence;

    // true nếu LLM thấy không thể map sang genre/tag cụ thể
    private Boolean fallbackToEmbedding;

    // LLM giải thích tại sao chọn những genre/tag này (debug)
    private String reasoning;
}