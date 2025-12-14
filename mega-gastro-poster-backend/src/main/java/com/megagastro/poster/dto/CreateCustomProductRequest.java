package com.megagastro.poster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCustomProductRequest {

    @NotBlank
    private String name;
    @NotNull
    private Double priceCurrent;
    private Double priceOriginal;
    private Integer discountPct;
    private String imageUrl;
    private String category;

}
