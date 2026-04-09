package com.thedryfruitsworld.controller;

import com.thedryfruitsworld.entity.Product;
import com.thedryfruitsworld.entity.Profile;
import com.thedryfruitsworld.entity.Review;
import com.thedryfruitsworld.exception.BadRequestException;
import com.thedryfruitsworld.exception.ResourceNotFoundException;
import com.thedryfruitsworld.repository.ProductRepository;
import com.thedryfruitsworld.repository.ProfileRepository;
import com.thedryfruitsworld.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Public + authenticated review endpoints.
 *
 * GET  /api/products/{slug}/reviews  — anyone
 * POST /api/products/{slug}/reviews  — authenticated customers (one review per product)
 */
@RestController
@RequestMapping("/api/products/{slug}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ProfileRepository profileRepository;

    public record ReviewDto(
            UUID id,
            int rating,
            String comment,
            String reviewerName,
            OffsetDateTime createdAt
    ) {}

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<ReviewDto>> list(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));

        Page<ReviewDto> dtos = reviewRepository
                .findByProductIdWithUser(product.getId(), PageRequest.of(page, limit))
                .map(r -> new ReviewDto(
                        r.getId(),
                        r.getRating(),
                        r.getComment(),
                        r.getUser() != null ? firstName(r.getUser().getFullName()) : "Customer",
                        r.getCreatedAt()
                ));

        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ReviewDto> submit(
            @PathVariable String slug,
            @RequestBody Map<String, Object> body,
            Authentication auth
    ) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        UUID userId = UUID.fromString(auth.getName());
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));

        if (reviewRepository.existsByProductIdAndUserId(product.getId(), userId)) {
            throw new BadRequestException("You have already reviewed this product");
        }

        Object ratingObj = body.get("rating");
        if (ratingObj == null) throw new BadRequestException("Rating is required");
        int rating = ((Number) ratingObj).intValue();
        if (rating < 1 || rating > 5) throw new BadRequestException("Rating must be between 1 and 5");

        Profile user = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(rating)
                .comment(body.get("comment") != null ? body.get("comment").toString().trim() : null)
                .build();

        reviewRepository.save(review);

        // Recalculate product average rating and review count
        long count = reviewRepository.countByProductId(product.getId());
        Double avg = reviewRepository.avgRatingByProductId(product.getId());
        product.setReviewCount((int) count);
        product.setRating(avg != null
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        productRepository.save(product);

        return ResponseEntity.ok(new ReviewDto(
                review.getId(),
                review.getRating(),
                review.getComment(),
                firstName(user.getFullName()),
                review.getCreatedAt()
        ));
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Customer";
        return fullName.split(" ")[0];
    }
}
