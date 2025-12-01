package com.megagastro.poster.controller;

import com.megagastro.poster.dto.PosterRequest;
import com.megagastro.poster.model.Product;
import com.megagastro.poster.service.PosterService;
import com.megagastro.poster.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/poster")
@CrossOrigin(origins = "*")
public class PosterController {

    private final PosterService posterService;
    private final ProductService productService;

    public PosterController(PosterService posterService, ProductService productService) {
        this.posterService = posterService;
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<byte[]> createPoster(@Valid @RequestBody PosterRequest request) {
        List<Product> selectedProducts = request.getProductIds().stream()
                .map(productService::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());

        int count = request.getCount() != null ? request.getCount() : 9;
        String title = request.getTitle() != null ? request.getTitle() : "Haftanın Fırsatları";

        String html = posterService.buildPosterHtml(title, selectedProducts, count);
        byte[] pdf = posterService.renderPdf(html);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"poster.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}


