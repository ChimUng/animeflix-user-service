package com.animeflix.animecatalogservice.config;

import com.animeflix.animecatalogservice.DTO.kafka.NewEpisodeEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, NewEpisodeEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Kafka broker address
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Key serializer (String - animeId)
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Value serializer (JSON - NewEpisodeEvent)
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");  // Wait for all replicas
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);   // Retry 3 times
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1); // Ordering guarantee

        // Performance settings
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        // Client ID for monitoring
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "catalog-service-producer");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, NewEpisodeEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}