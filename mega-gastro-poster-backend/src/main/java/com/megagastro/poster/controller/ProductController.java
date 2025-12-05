package com.megagastro.poster.controller;

import com.megagastro.poster.dto.CreateCustomProductRequest;
import com.megagastro.poster.model.Product;
import com.megagastro.poster.model.ProductSource;
import com.megagastro.poster.service.ProductService;
import jakarta.validation.Valid;
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
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/search")
    public List<Product> search(@RequestParam("q") String query) {
        return productService.searchProducts(query);
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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> uploadCustom(
            @RequestParam("name") String name,
            @RequestParam("priceCurrent") Double priceCurrent,
            @RequestParam(value = "priceOriginal", required = false) Double priceOriginal,
            @RequestParam(value = "discountPct", required = false) Integer discountPct,
            @RequestParam("file") MultipartFile file) throws IOException {
        // Proje root dizininde uploads klasörü oluştur
        String userDir = System.getProperty("user.dir");
        Path uploadsPath = Paths.get(userDir, "uploads");

        // Klasör yoksa oluştur
        if (!Files.exists(uploadsPath)) {
            Files.createDirectories(uploadsPath);
        }

        // Güvenli dosya adı oluştur
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "image";
        }
        // Dosya adındaki özel karakterleri temizle
        String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String filename = System.currentTimeMillis() + "_" + safeFilename;

        // Dosyayı kaydet
        Path destPath = uploadsPath.resolve(filename);
        Files.copy(file.getInputStream(), destPath, StandardCopyOption.REPLACE_EXISTING);

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
