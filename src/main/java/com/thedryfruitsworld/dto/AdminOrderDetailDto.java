package com.thedryfruitsworld.dto;

import com.thedryfruitsworld.entity.Order;
import com.thedryfruitsworld.entity.OrderItem;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Full order detail DTO for admin — includes items and address snapshot.
 */
public record AdminOrderDetailDto(
        UUID id,
        String status,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shipping,
        BigDecimal total,
        String paymentMethod,
        String razorpayOrderId,
        String razorpayPaymentId,
        OffsetDateTime createdAt,
        String customerName,
        String customerId,
        Map<String, String> addressSnapshot,
        List<OrderItemDto> items
) {
    public record OrderItemDto(
            UUID id,
            String productName,
            String variantLabel,
            int qty,
            BigDecimal unitPrice
    ) {
        public static OrderItemDto from(OrderItem item) {
            return new OrderItemDto(
                    item.getId(),
                    item.getProductName(),
                    item.getVariantLabel(),
                    item.getQty(),
                    item.getUnitPrice()
            );
        }
    }

    public static AdminOrderDetailDto from(Order o) {
        List<OrderItemDto> itemDtos = o.getItems() != null
                ? o.getItems().stream().map(OrderItemDto::from).toList()
                : List.of();

        return new AdminOrderDetailDto(
                o.getId(),
                o.getStatus(),
                o.getSubtotal(),
                o.getDiscount(),
                o.getShipping(),
                o.getTotal(),
                o.getPaymentMethod(),
                o.getRazorpayOrderId(),
                o.getRazorpayPaymentId(),
                o.getCreatedAt(),
                o.getUser() != null ? o.getUser().getFullName() : null,
                o.getUser() != null ? o.getUser().getId().toString() : null,
                o.getAddressSnapshot(),
                itemDtos
        );
    }
}
