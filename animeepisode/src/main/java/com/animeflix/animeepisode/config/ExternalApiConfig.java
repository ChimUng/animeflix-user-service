package com.animeflix.animeepisode.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ExternalApiConfig {

    @Value("${consumet.uri}")
    private String consumetUri;

    @Value("${malsync.uri}")
    private String malsyncUri;

    @Value("${zoro.uri}")
    private String zoroUri;

    @Value("${gogo.uri}")
    private String gogoUri;

    @Value("${zenime.url:https://zenime-api.vercel.app}")
    private String zenimeUrl;

    @Value("${anify.url}")
    private String anifyUrl;

    @Value("${animepahe.url}")
    private String animepaheUrl;

    @Value("${animapping.url}")
    private String animappingUrl;

    @Value("${anify.schedule.url}")
    private String anifyScheduleUrl;

    private ExchangeStrategies exchangeStrategies() {
        return ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();
    }

    @Bean
    public WebClient consumetWebClient() {
        return WebClient.builder()
                .baseUrl(consumetUri)
                .exchangeStrategies(exchangeStrategies())
                .build();
    }

    @Bean
    public WebClient malsyncWebClient() {
        return WebClient.builder().baseUrl(malsyncUri).exchangeStrategies(exchangeStrategies()).build();
    }

    @Bean
    public WebClient zoroWebClient() {
        return WebClient.builder()
                .baseUrl(zoroUri)
                .exchangeStrategies(exchangeStrategies())
                .build();
    }

    @Bean
    public WebClient gogoWebClient() {
        return WebClient.builder()
                .baseUrl(gogoUri)
                .exchangeStrategies(exchangeStrategies())
                .build();
    }

    @Bean
    public WebClient anifyWebClient() {
        return WebClient.builder().baseUrl(anifyUrl).exchangeStrategies(exchangeStrategies()).build();
    }

    @Bean
    public WebClient animepaheWebClient() {
        return WebClient.builder().baseUrl(animepaheUrl.replace("{anime_session}/{session}", "")).build();
    }

    @Bean
    public WebClient animappingWebClient() {
        String baseUrl = animappingUrl;
        if (baseUrl.contains("?")) {
            baseUrl = baseUrl.split("\\?")[0];
        }
        if (baseUrl.contains("/mappings")) {
            baseUrl = baseUrl.substring(0, baseUrl.indexOf("/mappings"));
        }
        return WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(exchangeStrategies())
                .build();
    }

    @Bean
    public WebClient nineAnimeWebClient() {
        return WebClient.builder()
                .baseUrl(zenimeUrl)
                .exchangeStrategies(exchangeStrategies())
                .build();
    }

    @Bean
    public WebClient anifyScheduleWebClient() {
        return WebClient.builder().baseUrl(anifyScheduleUrl).build();
    }
}