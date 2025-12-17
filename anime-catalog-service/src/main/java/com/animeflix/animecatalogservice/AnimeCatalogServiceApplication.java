package com.animeflix.animecatalogservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AnimeCatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnimeCatalogServiceApplication.class, args);
    }

}
