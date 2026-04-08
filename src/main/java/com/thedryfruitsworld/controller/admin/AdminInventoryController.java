package com.thedryfruitsworld.controller.admin;

import com.thedryfruitsworld.dto.InventoryItemDto;
import com.thedryfruitsworld.entity.ProductVariant;
import com.thedryfruitsworld.exception.BadRequestException;
import com.thedryfruitsworld.exception.ResourceNotFoundException;
import com.thedryfruitsworld.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final ProductVariantRepository variantRepository;

    /**
     * All variants with their parent product, ordered by product name then weight.
     */
    @GetMapping
    public ResponseEntity<List<InventoryItemDto>> list() {
        List<InventoryItemDto> items = variantRepository.findAllWithProduct()
                .stream()
                .map(InventoryItemDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    /**
     * Update stock quantity for a single variant.
     * Body: {@code { "stockQty": 50 }}
     */
    @PatchMapping("/{variantId}")
    @Transactional
    public ResponseEntity<InventoryItemDto> updateStock(
            @PathVariable UUID variantId,
            @RequestBody Map<String, Object> body
    ) {
        Object rawQty = body.get("stockQty");
        if (rawQty == null) {
            throw new BadRequestException("stockQty is required");
        }
        int qty;
        try {
            qty = Integer.parseInt(rawQty.toString());
        } catch (NumberFormatException e) {
            throw new BadRequestException("stockQty must be an integer");
        }
        if (qty < 0) {
            throw new BadRequestException("stockQty cannot be negative");
        }

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

        variant.setStockQty(qty);
        variantRepository.save(variant);
        return ResponseEntity.ok(new InventoryItemDto(variant));
    }
}
