package com.thedryfruitsworld.controller.admin;

import com.thedryfruitsworld.dto.AdminOrderDetailDto;
import com.thedryfruitsworld.dto.AdminOrderDto;
import com.thedryfruitsworld.entity.Order;
import com.thedryfruitsworld.exception.BadRequestException;
import com.thedryfruitsworld.exception.ResourceNotFoundException;
import com.thedryfruitsworld.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private static final Set<String> VALID_STATUSES = Set.of(
            "PENDING", "PROCESSING", "PACKED", "SHIPPED", "DELIVERED", "CANCELLED"
    );

    private final OrderRepository orderRepository;

    /**
     * Paginated list of all orders, optionally filtered by status.
     * Maps to AdminOrderDto within the transaction to avoid lazy-load issues.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<AdminOrderDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status
    ) {
        PageRequest pageable = PageRequest.of(page, limit);
        Page<Order> orders = status != null && !status.isBlank()
                ? orderRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable)
                : orderRepository.findAllByOrderByCreatedAtDesc(pageable);

        return ResponseEntity.ok(orders.map(AdminOrderDto::from));
    }

    /**
     * Full order detail — includes items and address snapshot.
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<AdminOrderDetailDto> detail(@PathVariable UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        return ResponseEntity.ok(AdminOrderDetailDto.from(order));
    }

    /**
     * Update order status.
     * Body: {@code { "status": "SHIPPED" }}
     */
    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<AdminOrderDto> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        String newStatus = body.get("status");
        if (newStatus == null || !VALID_STATUSES.contains(newStatus.toUpperCase())) {
            throw new BadRequestException("Invalid status. Must be one of: " + VALID_STATUSES);
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        order.setStatus(newStatus.toUpperCase());
        orderRepository.save(order);
        return ResponseEntity.ok(AdminOrderDto.from(order));
    }
}
