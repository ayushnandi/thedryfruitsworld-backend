package com.thedryfruitsworld.controller;

import com.thedryfruitsworld.entity.Order;
import com.thedryfruitsworld.exception.ResourceNotFoundException;
import com.thedryfruitsworld.repository.OrderRepository;
import com.thedryfruitsworld.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<Order>> myOrders(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(orderRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable UUID id,
                                          @AuthenticationPrincipal String userId) {
        Order order = orderRepository.findByIdAndUserId(id, UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return ResponseEntity.ok(order);
    }

    @PostMapping
    public ResponseEntity<Order> placeOrder(@RequestBody Map<String, Object> body,
                                            @AuthenticationPrincipal String userId) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        String paymentMethod = (String) body.get("paymentMethod");
        String couponCode = (String) body.get("couponCode");

        @SuppressWarnings("unchecked")
        Map<String, String> addressSnapshot = (Map<String, String>) body.get("address");

        Order order = orderService.createOrder(userId, items, paymentMethod, couponCode, addressSnapshot, null, null);
        return ResponseEntity.ok(order);
    }
}
