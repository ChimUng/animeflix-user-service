package com.animeflix.animeepisode.model.stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoTimeRange {
    private Integer start;
    private Integer end;
}
