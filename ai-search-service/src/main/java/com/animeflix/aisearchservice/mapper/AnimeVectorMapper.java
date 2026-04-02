package com.animeflix.aisearchservice.mapper;

import com.animeflix.aisearchservice.dto.response.AnimeSearchResultDTO;
import com.animeflix.aisearchservice.Entity.AnimeVector;
import org.springframework.stereotype.Component;

@Component
public class AnimeVectorMapper {

    public AnimeSearchResultDTO toSearchResult(AnimeVector av) {
        return AnimeSearchResultDTO.builder()
                .id(av.getId())
                .titleRomaji(av.getTitleRomaji())
                .titleEnglish(av.getTitleEnglish())
                .titleUserPreferred(av.getTitleUserPreferred())
                .coverImage(av.getCoverImageLarge())
                .bannerImage(av.getBannerImage())
                .genres(av.getGenres())
                .averageScore(av.getAverageScore())
                .popularity(av.getPopularity())
                .status(av.getStatus())
                .format(av.getFormat())
                .season(av.getSeason())
                .seasonYear(av.getSeasonYear())
                // similarityScore sẽ được gán sau khi vector search
                .similarityScore(null)
                .build();
    }
}