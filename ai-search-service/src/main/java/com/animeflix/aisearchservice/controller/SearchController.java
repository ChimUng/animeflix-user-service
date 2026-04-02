package com.animeflix.aisearchservice.controller;

import com.animeflix.aisearchservice.dto.request.SearchRequestDTO;
import com.animeflix.aisearchservice.dto.response.SearchResponseDTO;
import com.animeflix.aisearchservice.exception.ApiResponse;
import com.animeflix.aisearchservice.service.SearchOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SearchController {

    private final SearchOrchestratorService searchOrchestratorService;

    /**
     * Main AI search endpoint.
     * Query tự nhiên → LLM parse → Structured hoặc Semantic path → Reranked results
     *
     * POST /api/search
     * Body: { "query": "anime hài hước học đường", "userId": "abc123", "page": 1, "perPage": 20 }
     */
    @PostMapping
    public Mono<ResponseEntity<ApiResponse<SearchResponseDTO>>> search(
            @Valid @RequestBody SearchRequestDTO request,
            ServerWebExchange exchange) {

        // Lấy userId từ header nếu không có trong body (từ Gateway)
        if (request.getUserId() == null) {
            String userIdHeader = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            request.setUserId(userIdHeader);
        }

        return searchOrchestratorService.search(request)
                .map(result -> ResponseEntity.ok(ApiResponse.ok(result)))
                .onErrorResume(e -> {
                    log.error("❌ Search error: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(500)
                            .body(ApiResponse.error("Search failed: " + e.getMessage())));
                });
    }

    /**
     * GET version cho dễ test trên browser.
     * GET /api/search?query=anime+hài+hước&page=1&perPage=20
     */
    @GetMapping
    public Mono<ResponseEntity<ApiResponse<SearchResponseDTO>>> searchGet(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage,
            ServerWebExchange exchange) {

        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");

        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery(query);
        request.setUserId(userId);
        request.setPage(page);
        request.setPerPage(perPage);

        return searchOrchestratorService.search(request)
                .map(result -> ResponseEntity.ok(ApiResponse.ok(result)))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500)
                        .body(ApiResponse.error(e.getMessage()))));
    }
}