package com.animeflix.animestream.Model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Model for Anify provider data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnifyProvider {
    private String id;
    private String url;
    private int priority;
    private String overrideUrl;
}