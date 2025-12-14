package com.megagastro.poster.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Product {

    private String id;
    private String name;
    private String url;
    private String imageUrl;
    private Double priceCurrent;
    private Double priceOriginal;
    private Integer discountPct;
    private ProductSource source;
    private String category;

}
