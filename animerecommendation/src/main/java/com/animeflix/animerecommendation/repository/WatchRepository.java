package com.animeflix.animerecommendation.repository;

import com.animeflix.animerecommendation.model.Watch;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchRepository extends MongoRepository<Watch, String> {
    List<Watch> findByUserNameOrderByCreatedAtDesc(String userName);
}
