package com.animeflix.animestream.Model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Model for subtitle data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subtitle {
    private String url;
    private String lang;
}