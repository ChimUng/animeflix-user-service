package com.animeflix.animestream.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MalSyncEntry {
    private String providerId;
    private String sub;
    private String dub;
}