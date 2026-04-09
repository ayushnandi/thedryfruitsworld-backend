package com.thedryfruitsworld.controller.admin;

import com.thedryfruitsworld.entity.Category;
import com.thedryfruitsworld.entity.Product;
import com.thedryfruitsworld.entity.ProductImage;
import com.thedryfruitsworld.entity.ProductVariant;
import com.thedryfruitsworld.exception.BadRequestException;
import com.thedryfruitsworld.exception.ResourceNotFoundException;
import com.thedryfruitsworld.repository.CategoryRepository;
import com.thedryfruitsworld.repository.ProductImageRepository;
import com.thedryfruitsworld.repository.ProductRepository;
import com.thedryfruitsworld.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin endpoints for full product lifecycle management.
 *
 * All routes are under /api/admin/products and require the ADMIN or MANAGER
 * role, enforced by SecurityConfig at the filter chain level.
 *
 * Design notes:
 *  - Repositories are injected directly (no service layer) per project convention.
 *  - Slug is auto-derived from name when omitted: lowercase, spaces → hyphens.
 *  - Paginated list returns ALL products (active and inactive) so admins can
 *    find and reactivate products that are hidden from the public store.
 *  - toggle-active is a dedicated endpoint so frontends can flip status with a
 *    single PUT and no risk of accidentally overwriting other fields.
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    /**
     * Paginated list of ALL products (active and inactive).
     *
     * @param page  zero-based page index (default 0)
     * @param limit page size (default 20)
     * @param q     optional free-text search on name / shortDescription
     */
    @GetMapping
    public ResponseEntity<Page<Product>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String q
    ) {
        PageRequest pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());

        Page<Product> result = (q != null && !q.isBlank())
                ? productRepository.adminSearch(q, pageable)
                : productRepository.findAll(pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * Fetch a single product by UUID, including inactive ones.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> detail(@PathVariable UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return ResponseEntity.ok(product);
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    /**
     * Create a new product.
     *
     * Expected JSON body fields (all optional unless noted):
     *   name (required), slug, categoryId (UUID), shortDescription,
     *   description, isBestseller, isActive
     *
     * If slug is omitted it is auto-generated from name.
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Product> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Product name is required");
        }

        String slug = resolveSlug(body, name);
        Category category = resolveCategory(body);

        Product product = Product.builder()
                .name(name.trim())
                .slug(slug)
                .category(category)
                .shortDescription((String) body.get("shortDescription"))
                .description((String) body.get("description"))
                .isBestseller(Boolean.TRUE.equals(body.get("isBestseller")))
                .isActive(body.get("isActive") == null || Boolean.TRUE.equals(body.get("isActive")))
                .build();

        return ResponseEntity.ok(productRepository.save(product));
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    /**
     * Full update of a product. Fields omitted from the request body are
     * reset to their null/default equivalents — callers should send the
     * complete representation (PUT semantics).
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Product> update(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body
    ) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        String name = (String) body.get("name");
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Product name is required");
        }

        String slug = resolveSlug(body, name);
        Category category = resolveCategory(body);

        product.setName(name.trim());
        product.setSlug(slug);
        product.setCategory(category);
        product.setShortDescription((String) body.get("shortDescription"));
        product.setDescription((String) body.get("description"));
        product.setBestseller(Boolean.TRUE.equals(body.get("isBestseller")));
        product.setActive(body.get("isActive") == null || Boolean.TRUE.equals(body.get("isActive")));

        return ResponseEntity.ok(productRepository.save(product));
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    /**
     * Permanently delete a product by UUID.
     * Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        productRepository.delete(product);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // TOGGLE ACTIVE
    // -------------------------------------------------------------------------

    /**
     * Flip the isActive flag without touching any other field.
     * Useful for quick enable/disable from admin list views.
     */
    @PutMapping("/{id}/toggle-active")
    @Transactional
    public ResponseEntity<Product> toggleActive(@PathVariable UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setActive(!product.isActive());
        return ResponseEntity.ok(productRepository.save(product));
    }

    // -------------------------------------------------------------------------
    // VARIANTS
    // -------------------------------------------------------------------------

    @GetMapping("/{id}/variants")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProductVariant>> listVariants(@PathVariable UUID id) {
        productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return ResponseEntity.ok(variantRepository.findByProductIdOrderByWeightGramsAsc(id));
    }

    @PostMapping("/{id}/variants")
    @Transactional
    public ResponseEntity<ProductVariant> addVariant(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body
    ) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        String label = (String) body.get("label");
        String sku = (String) body.get("sku");
        if (label == null || label.isBlank()) throw new BadRequestException("Variant label is required");
        if (sku == null || sku.isBlank()) throw new BadRequestException("SKU is required");

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .label(label.trim())
                .sku(sku.trim())
                .weightGrams(body.get("weightGrams") != null ? ((Number) body.get("weightGrams")).intValue() : 0)
                .price(new BigDecimal(body.get("price").toString()))
                .mrp(new BigDecimal(body.get("mrp").toString()))
                .stockQty(body.get("stockQty") != null ? ((Number) body.get("stockQty")).intValue() : 0)
                .isActive(body.get("isActive") == null || Boolean.TRUE.equals(body.get("isActive")))
                .build();

        return ResponseEntity.ok(variantRepository.save(variant));
    }

    @PutMapping("/variants/{variantId}")
    @Transactional
    public ResponseEntity<ProductVariant> updateVariant(
            @PathVariable UUID variantId,
            @RequestBody Map<String, Object> body
    ) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

        if (body.get("label") != null) variant.setLabel(body.get("label").toString().trim());
        if (body.get("sku") != null) variant.setSku(body.get("sku").toString().trim());
        if (body.get("weightGrams") != null) variant.setWeightGrams(((Number) body.get("weightGrams")).intValue());
        if (body.get("price") != null) variant.setPrice(new BigDecimal(body.get("price").toString()));
        if (body.get("mrp") != null) variant.setMrp(new BigDecimal(body.get("mrp").toString()));
        if (body.get("stockQty") != null) variant.setStockQty(((Number) body.get("stockQty")).intValue());
        if (body.get("isActive") != null) variant.setActive(Boolean.TRUE.equals(body.get("isActive")));

        return ResponseEntity.ok(variantRepository.save(variant));
    }

    @DeleteMapping("/variants/{variantId}")
    @Transactional
    public ResponseEntity<Void> deleteVariant(@PathVariable UUID variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));
        variantRepository.delete(variant);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // IMAGES
    // -------------------------------------------------------------------------

    @GetMapping("/{id}/images")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProductImage>> listImages(@PathVariable UUID id) {
        productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return ResponseEntity.ok(imageRepository.findByProductIdOrderBySortOrderAsc(id));
    }

    @PostMapping("/{id}/images")
    @Transactional
    public ResponseEntity<ProductImage> addImage(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body
    ) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        String url = (String) body.get("url");
        if (url == null || url.isBlank()) throw new BadRequestException("Image URL is required");

        boolean isPrimary = Boolean.TRUE.equals(body.get("isPrimary"));
        if (isPrimary) {
            // Clear existing primary flag
            imageRepository.findByProductIdOrderBySortOrderAsc(id)
                    .forEach(img -> { img.setPrimary(false); imageRepository.save(img); });
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .url(url.trim())
                .alt(body.get("alt") != null ? body.get("alt").toString() : null)
                .isPrimary(isPrimary)
                .sortOrder(body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : 0)
                .build();

        return ResponseEntity.ok(imageRepository.save(image));
    }

    @DeleteMapping("/images/{imageId}")
    @Transactional
    public ResponseEntity<Void> deleteImage(@PathVariable UUID imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));
        imageRepository.delete(image);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Return the slug from the request body if present and non-blank,
     * otherwise derive it from the product name.
     */
    private String resolveSlug(Map<String, Object> body, String name) {
        String slug = (String) body.get("slug");
        if (slug == null || slug.isBlank()) {
            slug = name.trim().toLowerCase().replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", "");
        }
        return slug.trim();
    }

    /**
     * Look up a Category from the categoryId field in the request body.
     * Returns null if categoryId is absent — allows products without a category.
     */
    private Category resolveCategory(Map<String, Object> body) {
        Object rawCategoryId = body.get("categoryId");
        if (rawCategoryId == null) {
            return null;
        }
        UUID categoryId = UUID.fromString(rawCategoryId.toString());
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }
}
