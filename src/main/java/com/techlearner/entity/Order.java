package com.techlearner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════
 * Order Entity - N+1 Problem Demo
 * ═══════════════════════════════════════════════════════════════
 *
 * TOPIC: N+1 Problem Deep Dive
 * ─────────────────────────────────────
 * Scenario: Sabhi orders fetch karo with user info.
 *
 * BAD way (N+1):
 *   List<Order> orders = orderRepo.findAll();
 *   for (Order o : orders) {
 *       System.out.println(o.getUser().getName()); // Each call = 1 extra query!
 *   }
 *   Total queries: 1 (orders) + N (one per user) = N+1
 *
 * GOOD way (JOIN FETCH):
 *   SELECT o FROM Order o JOIN FETCH o.user WHERE ...
 *   Total queries: 1 (single JOIN query)
 *
 * BETTER way (EntityGraph):
 *   @EntityGraph(attributePaths = {"user", "items"})
 *   findAllWithUserAndItems()
 *
 * BEST for large collections (Batch Fetch):
 *   spring.jpa.properties.hibernate.default_batch_fetch_size=10
 *   → Hibernate will batch: SELECT * FROM users WHERE id IN (1,2,...,10)
 *
 * TOPIC: Pagination Strategies
 * ─────────────────────────────────────
 * Strategy 1: OFFSET Pagination (simple but slow for large offsets)
 *   SELECT * FROM orders LIMIT 10 OFFSET 10000
 *   Problem: DB scans 10010 rows, throws away 10000!
 *
 * Strategy 2: CURSOR/KEYSET Pagination (fast, stable)
 *   SELECT * FROM orders WHERE id > :lastId LIMIT 10
 *   Uses index! No offset scan needed.
 *
 * Strategy 3: Seek Method (for complex sorting)
 *   WHERE (created_at, id) < (:lastDate, :lastId)
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user_id", columnList = "user_id"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * TOPIC: DB Transactions & Isolation
     * ─────────────────────────────────────
     * @ManyToOne with LAZY fetch.
     * Transaction ke andar access karo → works fine.
     * Transaction ke bahar access karo → LazyInitializationException!
     * Fix: Use DTO projection or JOIN FETCH.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum OrderStatus {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }
}
