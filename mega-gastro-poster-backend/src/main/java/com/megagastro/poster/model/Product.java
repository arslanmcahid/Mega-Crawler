package com.megagastro.poster.model;

public class Product {

    private String id;
    private String name;
    private String url;
    private String imageUrl;
    private Double priceCurrent;
    private Double priceOriginal;
    private Integer discountPct;
    private ProductSource source;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public ProductSource getSource() {
        return source;
    }

    public void setSource(ProductSource source) {
        this.source = source;
    }
}


