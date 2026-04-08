package com.thedryfruitsworld.service;

import com.thedryfruitsworld.entity.*;
import com.thedryfruitsworld.exception.BadRequestException;
import com.thedryfruitsworld.exception.ResourceNotFoundException;
import com.thedryfruitsworld.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProfileRepository profileRepository;
    private final ProductVariantRepository variantRepository;
    private final CouponRepository couponRepository;

    /**
     * Creates and persists an order.
     *
     * @param userId             authenticated user's UUID string
     * @param items              list of {variantId, qty} maps
     * @param paymentMethod      "COD" or "ONLINE"
     * @param couponCode         optional coupon code
     * @param addressSnapshot    address fields to store as JSONB
     * @param razorpayOrderId    set for online payments (null for COD)
     * @param razorpayPaymentId  set after payment verified (null otherwise)
     */
    @Transactional
    public Order createOrder(String userId,
                              List<Map<String, Object>> items,
                              String paymentMethod,
                              String couponCode,
                              Map<String, String> addressSnapshot,
                              String razorpayOrderId,
                              String razorpayPaymentId) {

        Profile profile = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map<String, Object> item : items) {
            UUID variantId = UUID.fromString((String) item.get("variantId"));
            int qty = Integer.parseInt(item.get("qty").toString());

            ProductVariant variant = variantRepository.findByIdWithLock(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

            if (variant.getStockQty() < qty) {
                throw new BadRequestException(
                        "Insufficient stock for " + variant.getProduct().getName() + " " + variant.getLabel());
            }

            variant.setStockQty(variant.getStockQty() - qty);
            variantRepository.save(variant);

            BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(qty));
            subtotal = subtotal.add(lineTotal);

            orderItems.add(OrderItem.builder()
                    .variant(variant)
                    .productName(variant.getProduct().getName())
                    .variantLabel(variant.getLabel())
                    .qty(qty)
                    .unitPrice(variant.getPrice())
                    .build());
        }

        // Apply coupon discount
        BigDecimal discount = BigDecimal.ZERO;
        Coupon coupon = null;
        if (couponCode != null && !couponCode.isBlank()) {
            coupon = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(couponCode).orElse(null);
            if (coupon != null) {
                if (coupon.getExpiresAt() == null || coupon.getExpiresAt().isAfter(OffsetDateTime.now())) {
                    if (subtotal.compareTo(coupon.getMinOrder()) >= 0) {
                        if ("PERCENTAGE".equals(coupon.getType())) {
                            discount = subtotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100));
                        } else {
                            discount = coupon.getValue();
                        }
                        coupon.setUsedCount(coupon.getUsedCount() + 1);
                        couponRepository.save(coupon);
                    }
                }
            }
        }

        BigDecimal freeShippingThreshold = new BigDecimal("499");
        BigDecimal shipping = subtotal.subtract(discount).compareTo(freeShippingThreshold) >= 0
                ? BigDecimal.ZERO : new BigDecimal("99");
        BigDecimal total = subtotal.subtract(discount).add(shipping);

        // Determine status:
        // - COD → PROCESSING immediately
        // - Online with verified payment (razorpayPaymentId present) → PROCESSING
        // - Online not yet verified → PENDING
        String status = ("COD".equals(paymentMethod) || razorpayPaymentId != null)
                ? "PROCESSING" : "PENDING";

        Order order = Order.builder()
                .user(profile)
                .status(status)
                .subtotal(subtotal)
                .discount(discount)
                .shipping(shipping)
                .total(total)
                .coupon(coupon)
                .addressSnapshot(addressSnapshot)
                .paymentMethod(paymentMethod)
                .razorpayOrderId(razorpayOrderId)
                .razorpayPaymentId(razorpayPaymentId)
                .build();

        order = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        order.setItems(orderItems);
        orderRepository.save(order);

        return order;
    }
}
