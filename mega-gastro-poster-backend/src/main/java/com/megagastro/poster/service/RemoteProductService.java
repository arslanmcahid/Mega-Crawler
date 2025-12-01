package com.megagastro.poster.service;

import com.megagastro.poster.model.Product;
import com.megagastro.poster.model.ProductSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RemoteProductService {

    private final RestClient restClient;

    public RemoteProductService(@Value("${crawler.base-url}") String crawlerBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(crawlerBaseUrl)
                .build();
    }

    public List<Product> fetchRemoteProducts() {
        RemoteProductDto[] response = restClient.get()
                .uri("/products")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(RemoteProductDto[].class);

        if (response == null) {
            return List.of();
        }

        return Arrays.stream(response)
                .filter(Objects::nonNull)
                .map(this::mapToProduct)
                .collect(Collectors.toList());
    }

    private Product mapToProduct(RemoteProductDto dto) {
        Product p = new Product();
        String url = dto.url != null ? dto.url : "";
        p.setId("remote-" + url.hashCode());
        p.setName(dto.name);
        p.setUrl(url);
        p.setImageUrl(dto.image_url);
        p.setPriceCurrent(dto.price_current != null ? dto.price_current : 0.0);
        p.setPriceOriginal(dto.price_original != null ? dto.price_original : p.getPriceCurrent());
        p.setDiscountPct(dto.discount_pct != null ? dto.discount_pct : 0);
        p.setSource(ProductSource.REMOTE);
        return p;
    }

    private static class RemoteProductDto {
        public String name;
        public String url;
        public String image_url;
        public Double price_current;
        public Double price_original;
        public Integer discount_pct;
    }
}


