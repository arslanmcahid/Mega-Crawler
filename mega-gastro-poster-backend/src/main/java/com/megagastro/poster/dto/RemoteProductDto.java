package com.megagastro.poster.dto;

public record RemoteProductDto(
        String name,
        String url,
        String image_url,
        double price_current,
        double price_original,
        int discount_pct,
        String category
) {}
