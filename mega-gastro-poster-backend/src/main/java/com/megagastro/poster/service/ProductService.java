package com.megagastro.poster.service;

import com.megagastro.poster.model.Product;
import com.megagastro.poster.repository.CustomProductRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final RemoteProductService remoteProductService;
    private final CustomProductRepository customProductRepository;

    public ProductService(RemoteProductService remoteProductService,
                          CustomProductRepository customProductRepository) {
        this.remoteProductService = remoteProductService;
        this.customProductRepository = customProductRepository;
    }

    public List<Product> getAllProducts() {
        List<Product> all = new ArrayList<>();
        all.addAll(remoteProductService.fetchRemoteProducts());
        all.addAll(customProductRepository.findAll());
        return all;
    }

    public Optional<Product> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        if (id.startsWith("remote-")) {
            return remoteProductService.fetchRemoteProducts().stream()
                    .filter(p -> id.equals(p.getId()))
                    .findFirst();
        }
        return customProductRepository.findById(id);
    }

    public Product saveCustom(Product p) {
        return customProductRepository.save(p);
    }

    public List<Product> searchProducts(String query) {
        if (query == null || query.trim().isEmpty() || query.trim().length() < 3) {
            return List.of();
        }
        
        String searchTerm = query.toLowerCase().trim();
        List<Product> all = new ArrayList<>();
        all.addAll(remoteProductService.fetchRemoteProducts());
        all.addAll(customProductRepository.findAll());
        
        return all.stream()
                .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(searchTerm))
                .toList();
    }
}


