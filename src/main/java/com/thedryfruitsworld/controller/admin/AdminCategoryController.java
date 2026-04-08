package com.thedryfruitsworld.controller.admin;

import com.thedryfruitsworld.entity.Category;
import com.thedryfruitsworld.exception.BadRequestException;
import com.thedryfruitsworld.exception.ResourceNotFoundException;
import com.thedryfruitsworld.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryRepository categoryRepository;

    /** All categories including inactive ones (unlike the public endpoint). */
    @GetMapping
    public ResponseEntity<List<Category>> list() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Category> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Category name is required");
        }
        String slug = resolveSlug(body, name);
        Category category = Category.builder()
                .name(name.trim())
                .slug(slug)
                .description((String) body.get("description"))
                .imageUrl((String) body.get("imageUrl"))
                .isActive(body.get("isActive") == null || Boolean.TRUE.equals(body.get("isActive")))
                .build();
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Category> update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Category name is required");
        }
        category.setName(name.trim());
        category.setSlug(resolveSlug(body, name));
        category.setDescription((String) body.get("description"));
        category.setImageUrl((String) body.get("imageUrl"));
        if (body.containsKey("isActive")) {
            category.setActive(Boolean.TRUE.equals(body.get("isActive")));
        }
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        categoryRepository.delete(category);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/toggle-active")
    @Transactional
    public ResponseEntity<Category> toggleActive(@PathVariable UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        category.setActive(!category.isActive());
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    private String resolveSlug(Map<String, Object> body, String name) {
        String slug = (String) body.get("slug");
        if (slug == null || slug.isBlank()) {
            slug = name.trim().toLowerCase().replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", "");
        }
        return slug.trim();
    }
}
