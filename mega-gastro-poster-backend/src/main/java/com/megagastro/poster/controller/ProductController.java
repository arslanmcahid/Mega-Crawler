package com.megagastro.poster.controller;

import com.megagastro.poster.dto.CategoryDto;
import com.megagastro.poster.dto.CreateCustomProductRequest;
import com.megagastro.poster.model.Product;
import com.megagastro.poster.model.ProductSource;
import com.megagastro.poster.service.CategoryService;
import com.megagastro.poster.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/search")
    public List<Product> search(@RequestParam("q") String query) { return productService.searchProducts(query); }

    @GetMapping
    public List<Product> getAll() { return productService.getAllProducts();}

    @GetMapping("/categories")
    public List<CategoryDto> categories(){ return categoryService.getAllCategories(); }

    @PostMapping
    public ResponseEntity<Product> createCustom(@Valid @RequestBody CreateCustomProductRequest request) {
        Product saved = productService.createCustomProduct(request);
        return ResponseEntity
                .created(URI.create("/api/products/" + saved.getId()))
                .body(saved);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> uploadCustom(
            @RequestParam("name") String name,
            @RequestParam("priceCurrent") Double priceCurrent,
            @RequestParam(value = "priceOriginal", required = false) Double priceOriginal,
            @RequestParam(value = "discountPct", required = false) Integer discountPct,
            @RequestParam("file") MultipartFile file) throws IOException {
        String userDir = System.getProperty("user.dir");
        Path uploadsPath = Paths.get(userDir, "uploads");

        if (!Files.exists(uploadsPath)) {
            Files.createDirectories(uploadsPath);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "image";
        }
        String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String filename = System.currentTimeMillis() + "_" + safeFilename;

        Path destPath = uploadsPath.resolve(filename);
        Files.copy(file.getInputStream(), destPath, StandardCopyOption.REPLACE_EXISTING);

        String imageUrl = "/uploads/" + filename;

        Product p = Product.builder().name(name).priceCurrent(priceCurrent)
                            .priceOriginal(priceOriginal != null ? priceOriginal : priceCurrent).discountPct(discountPct)
                            .imageUrl(imageUrl).source(ProductSource.CUSTOM).build();
        Product saved = productService.saveCustom(p);
        return ResponseEntity
                .created(URI.create("/api/products/" + saved.getId()))
                .body(saved);
    }
}
