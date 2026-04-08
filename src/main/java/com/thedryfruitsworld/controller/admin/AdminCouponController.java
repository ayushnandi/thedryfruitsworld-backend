package com.thedryfruitsworld.controller.admin;

import com.thedryfruitsworld.entity.Coupon;
import com.thedryfruitsworld.exception.BadRequestException;
import com.thedryfruitsworld.exception.ResourceNotFoundException;
import com.thedryfruitsworld.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponRepository couponRepository;

    @GetMapping
    public ResponseEntity<List<Coupon>> list() {
        return ResponseEntity.ok(couponRepository.findAll());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Coupon> create(@RequestBody Map<String, Object> body) {
        Coupon coupon = buildCoupon(new Coupon(), body);
        return ResponseEntity.ok(couponRepository.save(coupon));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Coupon> update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + id));
        buildCoupon(coupon, body);
        return ResponseEntity.ok(couponRepository.save(coupon));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + id));
        couponRepository.delete(coupon);
        return ResponseEntity.noContent().build();
    }

    private Coupon buildCoupon(Coupon coupon, Map<String, Object> body) {
        String code = (String) body.get("code");
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Coupon code is required");
        }
        String type = (String) body.get("type");
        if (!"PERCENTAGE".equals(type) && !"FLAT".equals(type)) {
            throw new BadRequestException("type must be PERCENTAGE or FLAT");
        }
        Object rawValue = body.get("value");
        if (rawValue == null) {
            throw new BadRequestException("value is required");
        }

        coupon.setCode(code.toUpperCase().trim());
        coupon.setType(type);
        coupon.setValue(new BigDecimal(rawValue.toString()));

        Object rawMin = body.get("minOrder");
        coupon.setMinOrder(rawMin != null ? new BigDecimal(rawMin.toString()) : BigDecimal.ZERO);

        Object rawMax = body.get("maxUses");
        coupon.setMaxUses(rawMax != null ? Integer.parseInt(rawMax.toString()) : null);

        Object rawExpires = body.get("expiresAt");
        coupon.setExpiresAt(rawExpires != null ? OffsetDateTime.parse(rawExpires.toString()) : null);

        coupon.setActive(body.get("isActive") == null || Boolean.TRUE.equals(body.get("isActive")));
        return coupon;
    }
}
