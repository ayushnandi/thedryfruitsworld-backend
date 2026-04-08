package com.thedryfruitsworld.repository;

import com.thedryfruitsworld.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCodeIgnoreCaseAndIsActiveTrue(String code);
}
