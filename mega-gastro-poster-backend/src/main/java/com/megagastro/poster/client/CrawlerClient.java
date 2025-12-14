package com.megagastro.poster.client;

import com.megagastro.poster.dto.CategoryDto;
import com.megagastro.poster.dto.RemoteProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class CrawlerClient {

    private final WebClient webClient;

    public CrawlerClient(@Value("${crawler.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Cacheable(value = "remoteCategories")
    public List<CategoryDto> getCategories() {
        return webClient.get()
                .uri("/categories")
                .retrieve()
                .bodyToFlux(CategoryDto.class)
                .collectList()
                .block();
    }

    @Cacheable(value = "remoteProducts", key = "#categoriesKey == null ? 'ALL' : #categoriesKey")
    public List<RemoteProductDto> getProducts(String categoriesKey) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/products");
                    if (categoriesKey != null && !categoriesKey.isBlank()) {
                        uriBuilder.queryParam("categories", categoriesKey);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToFlux(RemoteProductDto.class)
                .collectList()
                .block();
    }
}
