package com.megagastro.poster.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class PosterRequest {

    @NotEmpty
    private List<String> productIds;

    private String title;

    private Integer count;

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}


