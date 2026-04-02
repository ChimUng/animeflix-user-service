package com.animeflix.aisearchservice.service;

import com.animeflix.aisearchservice.Entity.AnimeVector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Vector search dùng MongoDB Atlas $vectorSearch.
 *
 * QUAN TRỌNG: Cần tạo vector index trên Atlas trước:
 * Collection: anime_vectors
 * Index name: vector_index_description
 * {
 *   "fields": [{
 *     "type": "vector",
 *     "path": "descriptionVector",
 *     "numDimensions": 768,
 *     "similarity": "cosine"
 *   }]
 * }
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingSearchService {

    private final MongoTemplate mongoTemplate;

    @Value("${search.embedding.vector-index-name}")
    private String vectorIndexName;

    @Value("${search.embedding.num-candidates}")
    private int numCandidates;

    @Value("${search.embedding.limit}")
    private int limit;

    /**
     * Tìm kiếm anime bằng cosine similarity với query vector.
     * Kết quả đã được sắp xếp theo score giảm dần từ Atlas.
     *
     * @param queryVector  768-dim vector từ Gemini embed
     * @param topK         Số kết quả cần lấy
     * @return List AnimeVector kèm score similarity
     */
    public Mono<List<AnimeVector>> search(List<Double> queryVector, int topK) {
        return Mono.fromCallable(() -> {
            // $vectorSearch stage - chỉ dùng được trên MongoDB Atlas
            Document vectorSearchStage = new Document("$vectorSearch", new Document()
                    .append("index", vectorIndexName)
                    .append("path", "descriptionVector")
                    .append("queryVector", queryVector)
                    .append("numCandidates", numCandidates)  // Phải >= limit
                    .append("limit", Math.min(topK, limit))
            );

            // $addFields để lấy score similarity (Atlas tự tính)
            Document addScoreStage = new Document("$addFields", new Document()
                    .append("similarityScore", new Document("$meta", "vectorSearchScore"))
            );

            // Chạy aggregation
            List<Document> pipeline = List.of(vectorSearchStage, addScoreStage);

            List<Document> rawResults = mongoTemplate
                    .getCollection("anime_vectors")
                    .aggregate(pipeline)
                    .into(new java.util.ArrayList<>());

            // Map Document → AnimeVector
            return rawResults.stream()
                    .map(doc -> {
                        AnimeVector av = mongoTemplate.getConverter()
                                .read(AnimeVector.class, doc);
                        // Gắn similarityScore vào entity tạm (dùng để rerank)
                        // Score nằm trong doc, không có field trong entity
                        // → truyền qua wrapper hoặc dùng Map
                        return av;
                    })
                    .collect(java.util.stream.Collectors.toList());

        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Search với filter thêm (vd: chỉ tìm trong genres cụ thể)
     * Dùng khi cần kết hợp structured + semantic
     */
    public Mono<List<AnimeVector>> searchWithFilter(List<Double> queryVector, List<String> genreFilter, int topK) {
        return Mono.fromCallable(() -> {
            Document vectorSearchStage = new Document("$vectorSearch", new Document()
                    .append("index", vectorIndexName)
                    .append("path", "descriptionVector")
                    .append("queryVector", queryVector)
                    .append("numCandidates", numCandidates)
                    .append("limit", Math.min(topK, limit))
                    .append("filter", new Document("genres", new Document("$in", genreFilter)))
            );

            Document addScoreStage = new Document("$addFields", new Document()
                    .append("similarityScore", new Document("$meta", "vectorSearchScore"))
            );

            List<Document> pipeline = List.of(vectorSearchStage, addScoreStage);

            return mongoTemplate.getCollection("anime_vectors")
                    .aggregate(pipeline)
                    .into(new java.util.ArrayList<>())
                    .stream()
                    .map(doc -> mongoTemplate.getConverter().read(AnimeVector.class, doc))
                    .collect(java.util.stream.Collectors.toList());

        }).subscribeOn(Schedulers.boundedElastic());
    }
}