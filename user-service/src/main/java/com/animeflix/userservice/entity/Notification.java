package com.animeflix.userservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "user_created_idx", def = "{'userId': 1, 'createdAt': -1}")
@CompoundIndex(name = "user_read_idx", def = "{'userId': 1, 'isRead': 1}")
public class Notification {
    @Id
    private String id;

    @Indexed
    private String userId;

    // Notification type
    private NotificationType type;  // NEW_EPISODE, SYSTEM, RECOMMENDATION

    // Content
    private String title;
    private String message;
    private String imageUrl;

    // Related data
    private String animeId;
    private Integer episodeNumber;

    // Action URL (deeplink)
    private String actionUrl;       // "/anime/{id}/episode/{ep}"

    // Read status
    @Indexed
    private Boolean isRead;
    private LocalDateTime readAt;

    // Timestamps
    @Indexed
    private LocalDateTime createdAt;

    // TTL - Auto delete after 30 days
    @Indexed
    private Date expiresAt;

    public enum NotificationType {
        NEW_EPISODE,        // Tập mới của anime yêu thích
        SYSTEM,             // Thông báo hệ thống
        RECOMMENDATION,     // Gợi ý anime mới
        REMINDER            // Nhắc xem tiếp
    }
}
