package com.animeflix.aisearchservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponseDTO {
    private List<AnimeSearchResultDTO> results;
    private int totalCount;
    private int page;
    private int perPage;

    // "STRUCTURED" | "SEMANTIC" - để FE biết dùng path nào
    private String searchType;

    // Các genre/filter đã parse được (chỉ có khi searchType = STRUCTURED)
    private ParsedQueryDTO parsedQuery;
}