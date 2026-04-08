package com.thedryfruitsworld.dto;

import com.thedryfruitsworld.entity.Order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Flat DTO for the admin order list.
 * Avoids serializing the lazy-loaded {@code Order.user} (Profile) and
 * {@code Order.coupon} directly, which would cause either
 * LazyInitializationException or infinite recursion.
 */
public record AdminOrderDto(
        UUID id,
        String status,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shipping,
        BigDecimal total,
        String paymentMethod,
        OffsetDateTime createdAt,
        int itemCount,
        String customerName,
        String customerId
) {
    public static AdminOrderDto from(Order o) {
        return new AdminOrderDto(
                o.getId(),
                o.getStatus(),
                o.getSubtotal(),
                o.getDiscount(),
                o.getShipping(),
                o.getTotal(),
                o.getPaymentMethod(),
                o.getCreatedAt(),
                o.getItems() != null ? o.getItems().size() : 0,
                o.getUser() != null ? o.getUser().getFullName() : null,
                o.getUser() != null ? o.getUser().getId().toString() : null
        );
    }
}
