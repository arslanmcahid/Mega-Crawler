package com.megagastro.poster.service;

import com.megagastro.poster.dto.CategoryDto;
import com.megagastro.poster.dto.RemoteCategoryDto;
import com.megagastro.poster.model.Product;
import com.megagastro.poster.repository.CustomProductRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.C;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final RemoteProductService remoteProductService;
    private final CustomProductRepository customProductRepository;

    public List<CategoryDto> getAllCategories() {
        List<RemoteCategoryDto> remote = remoteProductService.fetchRemoteCategories();
        Map<String, CategoryDto> byKey = new LinkedHashMap<>();

        for(RemoteCategoryDto rc : remote) {
            if(rc == null) continue;
            String key = rc.key();
            String name = rc.name();
            String path = rc.path();

            if(key == null || key.isBlank()) { key = slugify(name != null ? name : "" );}
            if(name == null || name.isBlank()) { name = key;}
            byKey.putIfAbsent(key, new CategoryDto(key, name, path));
        }
        List<Product> customProducts = customProductRepository.findAll();
        Set<String> existingNames = byKey.values().stream()
                .map(CategoryDto::name).collect(Collectors.toSet());

        for(Product p : customProducts) {
            String name = p.getCategory();
            if(name == null) continue;
            name = name.trim();
            if(name.isEmpty()) continue;
            if(existingNames.contains(name)) continue;
            String key = slugify(name);
            if(!byKey.containsKey(key)){
                byKey.put(key, new CategoryDto(key,name, null));
                existingNames.add(name);
            }
        }
        return new ArrayList<>(byKey.values());
    }

    public Map<String, List<Product>> groupProductsByCategory(List<Product> products) {
        if(products == null || products.isEmpty()) return Collections.emptyMap();

        Map<String, List<Product>> grouped = products.stream().collect(Collectors.groupingBy( p -> {
            String category = p.getCategory();
            return (category == null || category.isBlank()) ? "Andere" : category;
        },
                LinkedHashMap::new, Collectors.toList()
        ));
        return grouped;
    }

    private String slugify(String input) {
        if(input == null) return "kategori";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.GERMAN).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "kategori" : slug;
    }

}
