package com.thedryfruitsworld.controller;

import com.thedryfruitsworld.entity.Product;
import com.thedryfruitsworld.exception.ResourceNotFoundException;
import com.thedryfruitsworld.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String q
    ) {
        Sort sortObj = switch (sort) {
            case "price_asc" -> Sort.by("variants.price").ascending();
            case "price_desc" -> Sort.by("variants.price").descending();
            case "rating" -> Sort.by("rating").descending();
            default -> Sort.by("createdAt").descending();
        };

        PageRequest pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        Page<Product> products;

        if (q != null && !q.isBlank()) {
            products = productRepository.search(q, pageable);
        } else if (category != null && !category.isBlank()) {
            products = productRepository.findByCategorySlug(category, pageable);
        } else {
            products = productRepository.findByIsActiveTrue(pageable);
        }

        return ResponseEntity.ok(Map.of(
                "content", products.getContent(),
                "totalElements", products.getTotalElements(),
                "totalPages", products.getTotalPages(),
                "page", page,
                "limit", limit
        ));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Product> detail(@PathVariable String slug) {
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
        return ResponseEntity.ok(product);
    }
}
