package com.animeflix.animetranslate.repository;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base repository tổng quát cho Supabase REST API
 */
public abstract class BaseSupabaseRepository<T> {

    protected final WebClient anonClient;
    protected final WebClient serviceClient;
    protected final String tableName;
    protected final Class<T> entityClass;

    protected BaseSupabaseRepository(WebClient anonClient, WebClient serviceClient, String tableName, Class<T> entityClass) {
        this.anonClient = anonClient;
        this.serviceClient = serviceClient;
        this.tableName = tableName;
        this.entityClass = entityClass;
    }

    /**
     * Lấy tất cả record
     */
    public Mono<List<T>> findAll() {
        return anonClient.get()
                .uri("/" + tableName + "?select=*")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(entityClass)
                .collectList();
    }

    /**
     * Lấy theo điều kiện (ví dụ: anilist_id=in.(1,2,3))
     */
    public Mono<List<T>> findByQuery(String filterQuery) {
        return anonClient.get()
                .uri("/" + tableName + "?select=*&" + filterQuery)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(entityClass)
                .collectList();
    }

    /**
     * Lấy theo ID (giả sử cột chính là id hoặc uuid)
     */
    public Mono<Optional<T>> findById(Object id) {
        return anonClient.get()
                .uri("/" + tableName + "?select=*&id=eq." + id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(entityClass)
                .collectList()
                .map(list -> list.isEmpty() ? Optional.empty() : Optional.of(list.get(0)));
    }

    /**
     * Upsert nhiều record
     */
    public Mono<Void> upsert(List<Map<String, Object>> data) {
        return serviceClient.post()
                .uri("/" + tableName)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(data)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> System.err.println("Upsert error: " + e.getMessage()))
                .onErrorComplete();
    }
}