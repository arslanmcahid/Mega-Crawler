package com.megagastro.poster.repository;

import com.megagastro.poster.model.Product;
import com.megagastro.poster.model.ProductSource;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CustomProductRepository {

    private final Map<String, Product> store = new ConcurrentHashMap<>();

    public Product save(Product p) {
        if (p.getId() == null || p.getId().isEmpty()) {
            p.setId(UUID.randomUUID().toString());
        }
        p.setSource(ProductSource.CUSTOM);
        store.put(p.getId(), p);
        return p;
    }

    public Optional<Product> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Product> findAll() {
        return new ArrayList<>(store.values());
    }
}


