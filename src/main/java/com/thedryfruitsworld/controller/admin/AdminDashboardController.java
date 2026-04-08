package com.thedryfruitsworld.controller.admin;

import com.thedryfruitsworld.dto.AdminOrderDto;
import com.thedryfruitsworld.dto.InventoryItemDto;
import com.thedryfruitsworld.repository.OrderRepository;
import com.thedryfruitsworld.repository.ProductRepository;
import com.thedryfruitsworld.repository.ProductVariantRepository;
import com.thedryfruitsworld.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProfileRepository profileRepository;

    public record DashboardResponse(
            BigDecimal totalRevenue,
            long totalOrders,
            long activeProducts,
            long totalUsers,
            List<AdminOrderDto> recentOrders,
            List<InventoryItemDto> lowStockVariants
    ) {}

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<DashboardResponse> dashboard() {
        BigDecimal revenue = orderRepository.sumRevenue();
        long totalOrders = orderRepository.count();
        long activeProducts = productRepository.countByIsActiveTrue();
        long totalUsers = profileRepository.count();

        List<AdminOrderDto> recentOrders = orderRepository
                .findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(AdminOrderDto::from)
                .collect(Collectors.toList());

        List<InventoryItemDto> lowStock = variantRepository
                .findLowStock(LOW_STOCK_THRESHOLD)
                .stream()
                .map(InventoryItemDto::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new DashboardResponse(
                revenue, totalOrders, activeProducts, totalUsers, recentOrders, lowStock
        ));
    }
}
