package com.megagastro.poster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateCustomProductRequest {

    @NotBlank
    private String name;

    @NotNull
    private Double priceCurrent;

    private Double priceOriginal;

    private Integer discountPct;

    private String imageUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPriceCurrent() {
        return priceCurrent;
    }

    public void setPriceCurrent(Double priceCurrent) {
        this.priceCurrent = priceCurrent;
    }

    public Double getPriceOriginal() {
        return priceOriginal;
    }

    public void setPriceOriginal(Double priceOriginal) {
        this.priceOriginal = priceOriginal;
    }

    public Integer getDiscountPct() {
        return discountPct;
    }

    public void setDiscountPct(Integer discountPct) {
        this.discountPct = discountPct;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}


