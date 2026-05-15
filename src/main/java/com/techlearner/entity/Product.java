package com.techlearner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ═══════════════════════════════════════════════════════════════
 * Product Entity
 * ═══════════════════════════════════════════════════════════════
 *
 * TOPIC: DB Indexing Internals (Composite Index)
 * ─────────────────────────────────────────────
 * Composite Index: (category, price) → useful for queries like:
 *   SELECT * FROM products WHERE category = 'ELECTRONICS' AND price < 500
 *
 * PostgreSQL Index Types:
 * - B-Tree (default) → range queries, equality, ORDER BY
 * - Hash             → only equality checks
 * - GIN/GiST        → full-text search, arrays
 * - BRIN            → time-series, sequential data
 *
 * TOPIC: Query Optimization
 * ─────────────────────────────────────────────
 * EXPLAIN ANALYZE karo PostgreSQL mein:
 *   EXPLAIN ANALYZE SELECT * FROM products WHERE category = 'ELECTRONICS';
 *
 * Seq Scan  → koi index nahi (slow for large tables)
 * Index Scan → index use kar raha hai (fast)
 * Bitmap Heap Scan → multiple conditions, hybrid approach
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_category", columnList = "category"),
        @Index(name = "idx_products_category_price", columnList = "category, price"),  // Composite index
        @Index(name = "idx_products_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private Integer stock = 0;

    @Version  // TOPIC: Optimistic Locking - prevents lost update problem
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
