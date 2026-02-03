package com.animeflix.animeepisode.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Custom deserializer for Provider.episodes field
 *
 * Episodes có thể là:
 * 1. List<Episode> - cho AnimePahe, 9anime
 * 2. Map<String, List<Episode>> - cho Zoro, Gogoanime (có "sub", "dub" keys)
 *
 * Deserializer detect structure và deserialize đúng type.
 */
public class EpisodesDeserializer extends JsonDeserializer<Object> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // Case 1: Array → List<Episode>
        if (node.isArray()) {
            return mapper.convertValue(node, new TypeReference<List<Episode>>() {});
        }

        // Case 2: Object với keys "sub", "dub" → Map<String, List<Episode>>
        if (node.isObject()) {
            // Check nếu có keys như "sub" hoặc "dub" → đây là Map
            if (node.has("sub") || node.has("dub")) {
                return mapper.convertValue(node, new TypeReference<Map<String, List<Episode>>>() {});
            }

            // Fallback: nếu object nhưng không có sub/dub → coi như empty Map
            return Map.of();
        }

        // Fallback: null hoặc unknown type
        return null;
    }
}