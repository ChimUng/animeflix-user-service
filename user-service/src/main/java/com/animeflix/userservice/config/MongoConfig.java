package com.animeflix.userservice.config;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    @Bean
    public CommandLineRunner createMongoIndexes() {
        return args -> {
            createNotificationTTLIndex();
            log.info("✅ MongoDB indexes created successfully");
        };
    }

    private void createNotificationTTLIndex() {
        try {
            MongoCollection<Document> collection = mongoTemplate.getCollection("notifications");

            IndexOptions indexOptions = new IndexOptions()
                    .expireAfter(0L, TimeUnit.SECONDS)
                    .name("expiresAt_ttl");

            collection.createIndex(
                    Indexes.ascending("expiresAt"),
                    indexOptions
            );

            log.info("✅ Created TTL index on notifications.expiresAt");

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                log.info("ℹ️ TTL index already exists");
            } else {
                log.error("❌ Error creating TTL index", e);
            }
        }
    }
}