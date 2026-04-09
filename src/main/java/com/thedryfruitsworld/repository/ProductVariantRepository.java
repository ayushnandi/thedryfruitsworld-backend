package com.thedryfruitsworld.repository;

import com.thedryfruitsworld.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> findByIdWithLock(@Param("id") UUID id);

    // Admin inventory: fetch all variants with their associated product (JOIN FETCH avoids N+1)
    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product p ORDER BY p.name ASC, v.weightGrams ASC")
    List<ProductVariant> findAllWithProduct();

    // Low-stock variants for dashboard (stockQty <= threshold)
    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product p WHERE v.stockQty <= :threshold ORDER BY v.stockQty ASC")
    List<ProductVariant> findLowStock(@Param("threshold") int threshold);

    List<ProductVariant> findByProductIdOrderByWeightGramsAsc(UUID productId);
}
