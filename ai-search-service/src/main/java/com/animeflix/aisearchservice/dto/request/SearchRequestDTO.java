package com.animeflix.aisearchservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequestDTO {

    @NotBlank(message = "Query không được để trống")
    private String query;

    private String userId;   // Optional - dùng để personalize

    @Min(1)
    private int page = 1;

    @Min(1) @Max(50)
    private int perPage = 20;
}