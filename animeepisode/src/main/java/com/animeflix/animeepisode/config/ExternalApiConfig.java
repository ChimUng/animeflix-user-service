package com.animeflix.animeepisode.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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

    private HttpClient createHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(30))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
                );
    }

    private HttpClient createMalSyncHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(15))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS))
                );
    }

    @Bean
    public WebClient consumetWebClient() {
        return WebClient.builder()
                .baseUrl(consumetUri)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .exchangeStrategies(exchangeStrategies())
                .build();
    }

    @Bean
    public WebClient malsyncWebClient() {
        return WebClient.builder()
                .baseUrl(malsyncUri)
                .clientConnector(new ReactorClientHttpConnector(createMalSyncHttpClient()))
                .exchangeStrategies(exchangeStrategies())
                .build();
    }

    @Bean
    public WebClient zoroWebClient() {
        return WebClient.builder()
                .baseUrl(zoroUri)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .exchangeStrategies(exchangeStrategies())
                .build();
    }

    @Bean
    public WebClient gogoWebClient() {
        return WebClient.builder()
                .baseUrl(gogoUri)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .exchangeStrategies(exchangeStrategies())
                .build();
    }

    @Bean
    public WebClient anifyWebClient() {
        return WebClient.builder()
                .baseUrl(anifyUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .exchangeStrategies(exchangeStrategies())
                .build();
    }

    @Bean
    public WebClient animepaheWebClient() {
        String cleanUrl = animepaheUrl.replace("{anime_session}/{session}", "");
        return WebClient.builder()
                .baseUrl(cleanUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .build();
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
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .exchangeStrategies(exchangeStrategies())
                .build();
    }

    @Bean
    public WebClient nineAnimeWebClient() {
        return WebClient.builder()
                .baseUrl(zenimeUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .exchangeStrategies(exchangeStrategies())
                .build();
    }


    @Bean
    public WebClient anifyScheduleWebClient() {
        return WebClient.builder()
                .baseUrl(anifyScheduleUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .build();
    }
}