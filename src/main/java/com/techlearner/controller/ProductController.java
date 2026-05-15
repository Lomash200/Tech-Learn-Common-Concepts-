package com.techlearner.controller;

import com.techlearner.aop.RateLimit;
import com.techlearner.dto.Dtos.*;
import com.techlearner.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Product Controller
 *
 * TOPIC: Spring Security - Method Level Security
 * @PreAuthorize("hasRole('ADMIN')") → AOP ke through check hota hai
 * SpEL (Spring Expression Language) use hota hai
 *
 * TOPIC: DispatcherServlet - Handler Mapping
 * /api/products → ProductController (registered via @RequestMapping)
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/products?page=0&size=10&sortBy=price
     *
     * TOPIC: Pagination Strategies
     * Query params se pagination control karo.
     *
     * TOPIC: Caching (Redis)
     * @Cacheable in service → second request cache se serve hoga
     * Check Redis: keys products::*
     */
    @GetMapping
    @RateLimit
    public ResponseEntity<PagedResponse<ProductResponse>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        return ResponseEntity.ok(productService.getProducts(page, size, sortBy));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * POST /api/products
     * ADMIN only!
     *
     * TOPIC: Spring Security @PreAuthorize
     * Method call se pehle SecurityContext check hoga.
     * Internally: AOP Proxy → MethodSecurityInterceptor → AccessDecisionManager
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/products/cursor?lastId=0&size=10
     * TOPIC: Cursor-based Pagination (Keyset Pagination)
     * Much faster than OFFSET for large datasets!
     */
    @GetMapping("/cursor")
    public ResponseEntity<List<ProductResponse>> getProductsCursor(
            @RequestParam(defaultValue = "0") Long lastId,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.getProductsAfterCursor(lastId, size));
    }

    /**
     * GET /api/products/category/{category}
     * TOPIC: Java Streams - filter + map + collect demo
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }
}
