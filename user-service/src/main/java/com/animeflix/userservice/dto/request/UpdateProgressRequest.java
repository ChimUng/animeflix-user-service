package com.animeflix.userservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProgressRequest {
    @NotBlank(message = "Anime ID is required")
    private String animeId;

    @NotBlank(message = "Episode ID is required")
    private String episodeId;

    @NotNull(message = "Episode number is required")
    private Integer episodeNumber;

    @NotNull(message = "Progress is required")
    @Min(0)
    @Max(1)
    private Double progress;

    @Min(0)
    private Integer watchedSeconds;

    @Min(0)
    private Integer totalSeconds;
}