package com.thedryfruitsworld.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "blog_posts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String excerpt;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "cover_image")
    private String coverImage;

    private String author;

    @Column(name = "author_role")
    private String authorRole;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "read_time")
    private Integer readTime;

    @Column(columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "is_published")
    private boolean isPublished = false;
}
