package com.animeflix.userservice.dto.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Event published khi có episode mới
 * Được publish bởi Catalog Service, consume bởi User Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEpisodeEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("anime_id")
    private String animeId;

    @JsonProperty("anime_title")
    private String animeTitle;

    @JsonProperty("episode_number")
    private Integer episodeNumber;

    @JsonProperty("airing_at")
    private Long airingAt;

    @JsonProperty("cover_image")
    private String coverImage;

    @JsonProperty("banner_image")
    private String bannerImage;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("source")
    private String source;
}