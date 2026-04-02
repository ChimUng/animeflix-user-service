package com.animeflix.aisearchservice.service;

import com.animeflix.aisearchservice.dto.request.SearchRequestDTO;
import com.animeflix.aisearchservice.dto.response.AnimeSearchResultDTO;
import com.animeflix.aisearchservice.dto.response.ParsedQueryDTO;
import com.animeflix.aisearchservice.dto.response.SearchResponseDTO;
import com.animeflix.aisearchservice.Entity.AnimeVector;
import com.animeflix.aisearchservice.mapper.AnimeVectorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Main orchestrator - điều phối toàn bộ flow:
 *
 * Query → [QueryParser] → confidence >= threshold?
 *   YES → [AniListAPI] → [UserHistory] → [ReRank Structured] → Response
 *   NO  → [GeminiEmbed] → [VectorSearch] → [UserHistory] → [ReRank Semantic] → Response
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchOrchestratorService {

    private final QueryParserService queryParserService;
    private final AniListApiService aniListApiService;
    private final EmbeddingSearchService embeddingSearchService;
    private final UserHistoryService userHistoryService;
    private final ReRankService reRankService;
    private final AnimeVectorMapper animeVectorMapper;
    private final com.animeflix.aisearchservice.client.GeminiClient geminiClient;

    public Mono<SearchResponseDTO> search(SearchRequestDTO request) {
        log.info("🔎 Search request: query='{}', userId='{}'",
                request.getQuery(), request.getUserId());

        // Step 1: Parse query với Gemini
        return queryParserService.parse(request.getQuery())
                .flatMap(parsed -> {

                    // Step 2: Quyết định path
                    if (Boolean.FALSE.equals(parsed.getFallbackToEmbedding())
                            && parsed.getConfidence() != null
                            && parsed.getConfidence() >= 0.75) {

                        log.info("🎯 Using STRUCTURED path (confidence={})", parsed.getConfidence());
                        return structuredPath(request, parsed);

                    } else {
                        log.info("🧠 Using SEMANTIC path (confidence={})", parsed.getConfidence());
                        return semanticPath(request, parsed);
                    }
                });
    }

    // ===================== STRUCTURED PATH =====================
    private Mono<SearchResponseDTO> structuredPath(SearchRequestDTO request, ParsedQueryDTO parsed) {
        return Mono.zip(
                // Parallel: AniList API + User genre preference
                aniListApiService.search(parsed, request.getPage(), request.getPerPage()),
                userHistoryService.getGenrePreference(request.getUserId())
        ).map(tuple -> {
            List<AnimeSearchResultDTO> results = tuple.getT1();
            Map<String, Integer> userPreference = tuple.getT2();

            // ReRank
            List<AnimeSearchResultDTO> reranked = reRankService.rerankStructured(results, userPreference);

            log.info("✅ Structured search done: {} results", reranked.size());
            return SearchResponseDTO.builder()
                    .results(reranked)
                    .totalCount(reranked.size())
                    .page(request.getPage())
                    .perPage(request.getPerPage())
                    .searchType("STRUCTURED")
                    .parsedQuery(parsed)
                    .build();
        });
    }

    // ===================== SEMANTIC PATH =====================
    private Mono<SearchResponseDTO> semanticPath(SearchRequestDTO request, ParsedQueryDTO parsed) {
        // Embed user query
        return geminiClient.embed(request.getQuery())
                .flatMap(queryVector ->
                        Mono.zip(
                                // Parallel: Vector search + User genre preference
                                embeddingSearchService.search(queryVector, request.getPerPage() * 3),
                                userHistoryService.getGenrePreference(request.getUserId())
                        )
                )
                .map(tuple -> {
                    List<AnimeVector> vectorResults = tuple.getT1();
                    Map<String, Integer> userPreference = tuple.getT2();

                    // Map AnimeVector → AnimeSearchResultDTO
                    List<AnimeSearchResultDTO> dtos = vectorResults.stream()
                            .map(animeVectorMapper::toSearchResult)
                            .collect(java.util.stream.Collectors.toList());

                    // ReRank
                    List<AnimeSearchResultDTO> reranked = reRankService.rerankSemantic(dtos, userPreference);

                    // Paginate (vector search trả về nhiều hơn cần)
                    int fromIndex = (request.getPage() - 1) * request.getPerPage();
                    int toIndex = Math.min(fromIndex + request.getPerPage(), reranked.size());
                    List<AnimeSearchResultDTO> paginated = (fromIndex < reranked.size())
                            ? reranked.subList(fromIndex, toIndex)
                            : List.of();

                    log.info("✅ Semantic search done: {} results (after rerank)", paginated.size());
                    return SearchResponseDTO.builder()
                            .results(paginated)
                            .totalCount(reranked.size())
                            .page(request.getPage())
                            .perPage(request.getPerPage())
                            .searchType("SEMANTIC")
                            .parsedQuery(parsed)
                            .build();
                });
    }
}