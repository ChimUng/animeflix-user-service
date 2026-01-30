package com.animeflix.animeepisode.mapper;

import com.animeflix.animeepisode.model.Episode;
import com.animeflix.animeepisode.model.EpisodeMeta;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mapper để merge metadata vào episode
 */
@Component
public class EpisodeMapper {

    /**
     * Merge metadata vào episode (in-place)
     */
    public void mergeMetadata(Episode episode, Map<String, EpisodeMeta> metaMap) {
        if (episode == null || episode.getNumber() == null) {
            return;
        }

        EpisodeMeta meta = metaMap.get(String.valueOf(episode.getNumber()));
        if (meta == null) {
            return;
        }

        // Title: ưu tiên "en" từ metadata
        Map<String, String> titleMap = meta.getTitle();
        if (titleMap != null && titleMap.containsKey("en")) {
            episode.setTitle(titleMap.get("en"));
        } else if (titleMap != null && !titleMap.isEmpty()) {
            // Fallback: lấy title đầu tiên
            episode.setTitle(titleMap.values().iterator().next());
        }

        // Image
        if (meta.getImage() != null && !meta.getImage().isEmpty()) {
            episode.setImage(meta.getImage());
        }

        // Description
        if (meta.getSummary() != null && !meta.getSummary().isEmpty()) {
            episode.setDescription(meta.getSummary());
        }
    }
}