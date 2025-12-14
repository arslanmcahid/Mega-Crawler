package com.megagastro.poster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class MegaGastroPosterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MegaGastroPosterApplication.class, args);
    }
}


