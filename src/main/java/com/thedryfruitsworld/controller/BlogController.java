package com.thedryfruitsworld.controller;

import com.thedryfruitsworld.entity.BlogPost;
import com.thedryfruitsworld.exception.ResourceNotFoundException;
import com.thedryfruitsworld.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogPostRepository blogPostRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<BlogPost> posts = blogPostRepository.findByIsPublishedTrue(
                PageRequest.of(page, limit, Sort.by("publishedAt").descending())
        );
        return ResponseEntity.ok(Map.of(
                "content", posts.getContent(),
                "totalElements", posts.getTotalElements(),
                "totalPages", posts.getTotalPages()
        ));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<BlogPost> detail(@PathVariable String slug) {
        BlogPost post = blogPostRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found: " + slug));
        return ResponseEntity.ok(post);
    }
}
