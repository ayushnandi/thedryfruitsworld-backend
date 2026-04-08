package com.thedryfruitsworld.repository;

import com.thedryfruitsworld.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {
    Page<BlogPost> findByIsPublishedTrue(Pageable pageable);
    Optional<BlogPost> findBySlugAndIsPublishedTrue(String slug);
}
