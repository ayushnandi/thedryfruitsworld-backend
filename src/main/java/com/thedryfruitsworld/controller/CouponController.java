package com.thedryfruitsworld.controller;

import com.thedryfruitsworld.entity.Coupon;
import com.thedryfruitsworld.exception.BadRequestException;
import com.thedryfruitsworld.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponRepository couponRepository;

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        BigDecimal orderTotal = new BigDecimal(body.get("orderTotal").toString());

        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code)
                .orElseThrow(() -> new BadRequestException("Invalid or expired coupon code"));

        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("This coupon has expired");
        }

        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new BadRequestException("This coupon has reached its usage limit");
        }

        if (orderTotal.compareTo(coupon.getMinOrder()) < 0) {
            throw new BadRequestException("Minimum order of ₹" + coupon.getMinOrder() + " required for this coupon");
        }

        BigDecimal discount;
        if ("PERCENTAGE".equals(coupon.getType())) {
            discount = orderTotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100));
        } else {
            discount = coupon.getValue();
        }

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "code", coupon.getCode(),
                "type", coupon.getType(),
                "value", coupon.getValue(),
                "discount", discount
        ));
    }
}
