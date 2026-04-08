package com.thedryfruitsworld.repository;

import com.thedryfruitsworld.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    // Admin: paginated list of all orders, optionally filtered by status
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    // Dashboard aggregates
    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status NOT IN ('CANCELLED', 'PENDING')")
    BigDecimal sumRevenue();

    List<Order> findTop5ByOrderByCreatedAtDesc();
}
