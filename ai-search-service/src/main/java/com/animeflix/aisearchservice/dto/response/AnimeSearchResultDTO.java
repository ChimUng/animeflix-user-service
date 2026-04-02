package com.animeflix.aisearchservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnimeSearchResultDTO {
    private String id;
    private String titleRomaji;
    private String titleEnglish;
    private String titleUserPreferred;
    private String coverImage;
    private String bannerImage;
    private List<String> genres;
    private Integer averageScore;
    private Integer popularity;
    private String status;
    private String format;
    private String season;
    private Integer seasonYear;

    // Chỉ có trong embedding path
    private Double similarityScore;

    // Final rerank score (tổng hợp)
    private Double rankScore;
}