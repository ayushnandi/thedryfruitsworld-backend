package com.thedryfruitsworld.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"variants", "images", "nutritionalInfo", "category", "hibernateLazyInitializer"})
    private Product product;

    @Column(name = "weight_grams", nullable = false)
    private int weightGrams;

    @Column(nullable = false)
    private String label;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal mrp;

    @Column(name = "stock_qty")
    private int stockQty = 0;

    @Column(name = "is_active")
    private boolean isActive = true;
}
