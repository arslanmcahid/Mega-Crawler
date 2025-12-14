package com.megagastro.poster.dto;

public record ProductDto(
        String name,
        String url,
        String imageUrl,
        double priceCurrent,
        double priceOriginal,
        int discountPct,
        String category,
        boolean custom
) {}

