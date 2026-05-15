package com.techlearner.service;

import com.techlearner.dto.Dtos.*;
import com.techlearner.entity.Product;
import com.techlearner.kafka.EventProducer;
import com.techlearner.kafka.ProductCreatedEvent;
import com.techlearner.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 * Product Service - Core Business Logic
 * ═══════════════════════════════════════════════════════════════
 *
 * TOPICS COVERED:
 * - @Transactional Internals
 * - DB Transactions & Isolation
 * - Caching (Redis) with @Cacheable
 * - Async Processing with @Async
 * - Java Streams Internals
 * - Kafka event publishing
 * - N+1 Problem awareness
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final EventProducer eventProducer;

    /**
     * ═══════════════════════════════════════════════════════════
     * TOPIC: @Transactional Internals
     * ═══════════════════════════════════════════════════════════
     *
     * @Transactional kaise kaam karta hai:
     * ─────────────────────────────────────
     * 1. AOP Proxy method intercept karta hai (TOPIC: Spring AOP)
     * 2. TransactionInterceptor → PlatformTransactionManager call
     * 3. PlatformTransactionManager (JpaTransactionManager):
     *    a. HikariCP se connection leta hai
     *    b. connection.setAutoCommit(false)
     *    c. Method execute hota hai
     *    d. Success → connection.commit()
     *    e. Exception → connection.rollback()
     *    f. connection pool ko wapas deta hai
     *
     * readOnly = true:
     * ─────────────────────────────────────
     * - Hibernate dirty checking skip karta hai (performance++)
     * - Some DBs read replica se serve kar sakte hain
     * - Flush mode NEVER set hoti hai
     *
     * TOPIC: DB Transactions & Isolation
     * Isolation.READ_COMMITTED (PostgreSQL default):
     * - Dirty reads nahi
     * - Non-repeatable reads possible
     * - Phantom reads possible
     */
    @Transactional(readOnly = true,
            isolation = Isolation.READ_COMMITTED)
    @Cacheable(value = "products",
            key = "'page_' + #page + '_size_' + #size + '_sort_' + #sortBy")
    public PagedResponse<ProductResponse> getProducts(int page, int size, String sortBy) {
        log.debug("Cache MISS - Loading products from DB: page={}, size={}, sort={}", page, size, sortBy);

        /*
         * TOPIC: Pagination Strategies
         * ─────────────────────────────────────
         * PageRequest.of(page, size, Sort) → OFFSET pagination
         * Internally: SELECT * FROM products ORDER BY ? LIMIT ? OFFSET ?
         *
         * Page vs Slice:
         * Page  → COUNT query bhi karta hai (for totalPages) → 2 queries!
         * Slice → Sirf data fetch karta hai, hasNext() check karta hai → 1 query (faster)
         * Use Slice jab total count zaruri nahi ho.
         */
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, sortBy));

        Page<Product> productPage = productRepository.findAll(pageable);

        /*
         * TOPIC: Java Streams Internals
         * ─────────────────────────────────────
         * Streams ek pipeline hai:
         * Source (findAll()) → Intermediate ops (map, filter) → Terminal op (collect)
         *
         * Lazy Evaluation:
         * filter + map tabhi execute hote hain jab collect() call hoti hai.
         * Intermediate operations khud kuch nahi karte.
         *
         * Stream Internals:
         * - Spliterator source se elements provide karta hai
         * - Pipeline: ReferencePipeline chain
         * - Terminal op: forEach/collect pipeline execute karta hai
         *
         * Parallel Streams:
         * .parallel() → ForkJoinPool.commonPool() use karta hai
         * Caution: Thread safety + overhead vs benefit consider karo
         */
        List<ProductResponse> responses = productPage.getContent()
                .stream()
                .map(this::toResponse)  // Method reference = lambda shorthand
                .collect(Collectors.toList());

        return PagedResponse.<ProductResponse>builder()
                .content(responses)
                .page(page)
                .size(size)
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    /**
     * TOPIC: Caching - @Cacheable
     * key = "#id" → Redis key: "products::1", "products::2" etc.
     * unless = skip caching if product is null
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id", unless = "#result == null")
    public ProductResponse getProductById(Long id) {
        log.debug("Cache MISS - Loading product {} from DB", id);
        return productRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    /**
     * TOPIC: @Transactional - REQUIRED propagation (default)
     * ─────────────────────────────────────
     * Propagation.REQUIRED:
     * - Transaction already hai? Use it.
     * - Nahi hai? Create new.
     *
     * Other Propagation types:
     * REQUIRES_NEW → Always new transaction (outer suspend hoti hai)
     * SUPPORTS    → Transaction ho to use, nahi ho to bhi chalo
     * NOT_SUPPORTED → Suspend outer transaction, run without
     * NEVER       → Transaction mein nahi chalega, exception throw
     * NESTED      → Savepoint ke saath nested (rollback partial possible)
     *
     * TOPIC: Caching - @CacheEvict
     * Product create hone pe "products" cache clear karo (stale data remove)
     */
    @Transactional(propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class)
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        // Check duplicate
        if (productRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Product already exists: " + request.getName());
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .stock(request.getStock())
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created: id={}, name={}", saved.getId(), saved.getName());

        // Async: Kafka event publish karo (non-blocking)
        // TOPIC: Async Processing
        publishProductCreatedEventAsync(saved);

        return toResponse(saved);
    }

    /**
     * TOPIC: Caching - @CachePut
     * Product update hone pe cache bhi update karo (replace stale data)
     * @CachePut always method execute karta hai (unlike @Cacheable which skips)
     *
     * TOPIC: @Transactional - Optimistic Locking
     * @Version field → concurrent updates detect karta hai
     * Agar do users same time update kare:
     * First update → version 1→2 (success)
     * Second update → version mismatch → OptimisticLockException thrown!
     */
    @Transactional
    @CachePut(value = "products", key = "#id")
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStock(request.getStock());

        // Dirty checking: Hibernate automatically UPDATE SQL generate karega
        // @Transactional ke bahar jaate hi flush hoga → commit
        // Explicit save() ki zarurat nahi (but doesn't hurt)
        Product updated = productRepository.save(product);
        return toResponse(updated);
    }

    /**
     * TOPIC: @Transactional - Rollback
     * RuntimeException → automatic rollback (default)
     * CheckedException → rollback nahi by default! Use rollbackFor = Exception.class
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found: " + id);
        }
        productRepository.deleteById(id);
        log.info("Product deleted: id={}", id);
    }

    /**
     * TOPIC: Async Processing + Java Streams
     * ─────────────────────────────────────
     * @Async → ye method ThreadPoolTaskExecutor ke ek thread pe run hoga
     * Caller immediately return ho jata hai
     * CompletableFuture → async result ya callback ke liye
     *
     * ThreadLocal Warning with @Async:
     * SecurityContextHolder ka data @Async mein available nahi hoga by default!
     * Fix: SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL)
     * ya manually context pass karo.
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> publishProductCreatedEventAsync(Product product) {
        try {
            log.debug("Async: Publishing Kafka event for product: {}", product.getId());
            eventProducer.publishProductCreated(ProductCreatedEvent.builder()
                    .productId(product.getId())
                    .name(product.getName())
                    .category(product.getCategory())
                    .price(product.getPrice())
                    .createdAt(LocalDateTime.now())
                    .build());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to publish Kafka event: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Cursor-based Pagination demo
     * TOPIC: Pagination Strategies - Keyset/Cursor pagination
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsAfterCursor(Long lastId, int size) {
        Pageable limit = PageRequest.of(0, size);
        return productRepository.findProductsAfter(lastId, limit)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Java Streams: filter + map + collect chain
     * TOPIC: Java Streams Internals
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(String category) {
        return productRepository.findAll()
                .stream()
                .filter(p -> p.getCategory().equalsIgnoreCase(category))  // Intermediate: lazy
                .filter(p -> p.getStock() > 0)                            // Intermediate: lazy
                .sorted((p1, p2) -> p1.getPrice().compareTo(p2.getPrice())) // Intermediate: lazy
                .map(this::toResponse)                                     // Intermediate: lazy
                .collect(Collectors.toList());                             // Terminal: triggers pipeline!
    }

    // ─── Mapper ────────────────────────────────────────────────
    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .category(p.getCategory())
                .stock(p.getStock())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
