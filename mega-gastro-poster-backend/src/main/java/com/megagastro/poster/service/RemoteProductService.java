package com.megagastro.poster.service;

import com.megagastro.poster.dto.RemoteCategoryDto;
import com.megagastro.poster.dto.RemoteProductDto;
import com.megagastro.poster.model.Product;
import com.megagastro.poster.model.ProductSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class RemoteProductService {

    private final WebClient webClient;

    public RemoteProductService(@Value("${crawler.base-url}") String crawlerBaseUrl,
                                WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(crawlerBaseUrl)
                .build();
    }

    /**
     * Crawler'dan ürünleri çek.
     * categories null/empty ise tüm kategorileri getirir.
     * Caffeine ile cache'lenir (CacheConfig'te 5 dk).
     */
    @Cacheable(
            value = "remoteProducts",
            key = "#categories == null || #categories.trim().isEmpty() ? 'ALL' : #categories.trim()"
    )
    public List<Product> fetchRemoteProducts(String categories) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/products");
                    if (categories != null && !categories.trim().isEmpty()) {
                        uriBuilder.queryParam("categories", categories.trim());
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToFlux(RemoteProductDto.class)
                .map(this::mapToProduct)
                .collectList()
                .block();
    }

    public List<Product> fetchRemoteProducts() {
        return fetchRemoteProducts(null);
    }

    /**
     * Crawler'dan kategori listesini çeker.
     * Bu da cache'lidir.
     */
    @Cacheable("remoteCategories")
    public List<RemoteCategoryDto> fetchRemoteCategories() {
        return webClient.get()
                .uri("/categories")
                .retrieve()
                .bodyToFlux(RemoteCategoryDto.class)
                .collectList()
                .block();
    }

    /**
     * Remote servis DTO'sunu kendi domain Product modeline çeviren yardımcı fonksiyon.
     * Burada:
     * - ID'yi kendimiz üretiyoruz (remote- hash)
     * - Source'u REMOTE olarak set ediyoruz
     * - Category ve fiyat alanlarını normalize ediyoruz
     */
    private Product mapToProduct(RemoteProductDto dto) {
        String url = dto.url() != null ? dto.url() : "";
        String name = dto.name() != null ? dto.name() : "";

        double current = dto.price_current();
        double original = dto.price_original() > 0 ? dto.price_original() : current;
        int discountPct = dto.discount_pct() > 0 ? dto.discount_pct() : 0;

        String category =
                dto.category() != null && !dto.category().isBlank()
                        ? dto.category()
                        : "Diğer";

        // Unique ID: URL + name + price üzerinden hash
        String uniqueKey = url + "|" + name + "|" + current;
        String id = "remote-" + Integer.toUnsignedString(uniqueKey.hashCode());
        return Product.builder()
                .id(id)
                .name(name)
                .url(url)
                .imageUrl(dto.image_url())
                .priceCurrent(current)
                .priceOriginal(original)
                .discountPct(discountPct)
                .source(ProductSource.REMOTE)
                .category(category)
                .build();
    }
}
