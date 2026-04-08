package com.thedryfruitsworld.dto;

import com.thedryfruitsworld.entity.ProductVariant;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Flat projection of a ProductVariant combined with its parent Product fields.
 * Used by the admin inventory endpoints and the dashboard low-stock summary.
 * Constructed directly from a ProductVariant that has its product JOIN FETCH-ed,
 * so no additional queries are triggered.
 */
@Getter
public class InventoryItemDto {

    private final UUID variantId;
    private final String sku;
    private final String label;
    private final BigDecimal price;
    private final BigDecimal mrp;
    private final int stockQty;
    private final UUID productId;
    private final String productName;
    private final String productSlug;

    public InventoryItemDto(ProductVariant v) {
        this.variantId    = v.getId();
        this.sku          = v.getSku();
        this.label        = v.getLabel();
        this.price        = v.getPrice();
        this.mrp          = v.getMrp();
        this.stockQty     = v.getStockQty();
        this.productId    = v.getProduct().getId();
        this.productName  = v.getProduct().getName();
        this.productSlug  = v.getProduct().getSlug();
    }
}
