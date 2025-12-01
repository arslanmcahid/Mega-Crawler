package com.megagastro.poster.controller;

import com.megagastro.poster.dto.CreateCustomProductRequest;
import com.megagastro.poster.model.Product;
import com.megagastro.poster.model.ProductSource;
import com.megagastro.poster.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getAll() {
        return productService.getAllProducts();
    }

    @PostMapping
    public ResponseEntity<Product> createCustom(@Valid @RequestBody CreateCustomProductRequest request) {
        Product p = new Product();
        p.setName(request.getName());
        p.setPriceCurrent(request.getPriceCurrent());
        p.setPriceOriginal(request.getPriceOriginal() != null ? request.getPriceOriginal() : request.getPriceCurrent());
        p.setDiscountPct(request.getDiscountPct());
        p.setImageUrl(request.getImageUrl());
        p.setSource(ProductSource.CUSTOM);

        Product saved = productService.saveCustom(p);
        return ResponseEntity
                .created(URI.create("/api/products/" + saved.getId()))
                .body(saved);
    }

    @PostMapping("/upload")
    public ResponseEntity<Product> uploadCustom(
            @RequestPart("name") String name,
            @RequestPart("priceCurrent") Double priceCurrent,
            @RequestPart(value = "priceOriginal", required = false) Double priceOriginal,
            @RequestPart(value = "discountPct", required = false) Integer discountPct,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        String uploadsDir = "uploads";
        File dir = new File(uploadsDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create uploads directory");
        }

        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File dest = new File(dir, filename);
        file.transferTo(dest);

        String imageUrl = "/uploads/" + filename;

        Product p = new Product();
        p.setName(name);
        p.setPriceCurrent(priceCurrent);
        p.setPriceOriginal(priceOriginal != null ? priceOriginal : priceCurrent);
        p.setDiscountPct(discountPct);
        p.setImageUrl(imageUrl);
        p.setSource(ProductSource.CUSTOM);

        Product saved = productService.saveCustom(p);
        return ResponseEntity
                .created(URI.create("/api/products/" + saved.getId()))
                .body(saved);
    }
}


