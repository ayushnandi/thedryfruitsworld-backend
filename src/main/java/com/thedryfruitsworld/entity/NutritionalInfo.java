package com.thedryfruitsworld.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "nutritional_info")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NutritionalInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", unique = true, nullable = false)
    private Product product;

    @Column(name = "serving_size")
    private String servingSize;

    private Integer calories;

    @Column(name = "protein_g", precision = 5, scale = 2)
    private BigDecimal proteinG;

    @Column(name = "carbs_g", precision = 5, scale = 2)
    private BigDecimal carbsG;

    @Column(name = "fat_g", precision = 5, scale = 2)
    private BigDecimal fatG;

    @Column(name = "fiber_g", precision = 5, scale = 2)
    private BigDecimal fiberG;

    @Column(name = "sugar_g", precision = 5, scale = 2)
    private BigDecimal sugarG;
}
