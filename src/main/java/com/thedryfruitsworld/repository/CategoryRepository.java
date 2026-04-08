package com.thedryfruitsworld.repository;

import com.thedryfruitsworld.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByIsActiveTrue();
    Optional<Category> findBySlug(String slug);
}
