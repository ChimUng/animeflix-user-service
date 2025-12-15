package com.animeflix.animerecommendation.service;

import com.animeflix.animerecommendation.model.Watch;
import com.animeflix.animerecommendation.repository.WatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WatchHistoryService {

    @Autowired
    private WatchRepository repository;

    public List<Watch> getWatchHistory(String userName) {
        return repository.findByUserNameOrderByCreatedAtDesc(userName);
    }
}