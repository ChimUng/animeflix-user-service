package com.animeflix.userservice.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message được push qua WebSocket đến frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketNotificationMessage {

    @JsonProperty("type")
    private String type;

    @JsonProperty("notification_id")
    private String notificationId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("message")
    private String message;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("anime_id")
    private String animeId;

    @JsonProperty("episode_number")
    private Integer episodeNumber;

    @JsonProperty("action_url")
    private String actionUrl;            // Deep link

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("priority")
    private String priority;             // "HIGH", "NORMAL", "LOW"
}