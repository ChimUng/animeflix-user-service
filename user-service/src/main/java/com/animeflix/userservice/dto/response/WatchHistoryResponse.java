package com.animeflix.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WatchHistoryResponse {
    private String id;
    private String aniId;
    private String aniTitle;
    private String image;

    private String epId;
    private Integer epNum;
    private String epTitle;

    private Double timeWatched;
    private Double duration;
    private Double progress;
    private Boolean completed;

    private String nextepId;
    private Integer nextepNum;

    private String provider;
    private String subtype;

    private String device;
    private String quality;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}