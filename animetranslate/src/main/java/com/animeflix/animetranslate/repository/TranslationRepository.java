package com.animeflix.animetranslate.repository;

import com.animeflix.animetranslate.model.TranslationResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class TranslationRepository extends BaseSupabaseRepository<TranslationResponse> {

    public TranslationRepository(@Qualifier("supabaseWebClient") WebClient anonClient,
                                 @Qualifier("supabaseServiceWebClient") WebClient serviceClient) {
        super(anonClient, serviceClient, "anime_translations", TranslationResponse.class);
    }

    public Mono<List<TranslationResponse>> getCachedTranslations(List<Integer> anilistIds) {
        String idsQuery = "anilist_id=in.(" + String.join(",", anilistIds.stream().map(String::valueOf).toList()) + ")";
        return findByQuery(idsQuery);
    }

    public Mono<Void> upsertTranslations(List<TranslationResponse> translations) {
        List<Map<String, Object>> data = translations.stream()
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("anilist_id", t.getAnilistId());
                    map.put("title_vi", t.getTitleVi());
                    map.put("description_vi", t.getDescriptionVi());
                    return map;
                })
                .collect(Collectors.toList());
        return upsert(data);
    }
}