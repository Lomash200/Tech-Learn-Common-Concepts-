package com.techlearner.repository;

import com.techlearner.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ═══════════════════════════════════════════════════════════════
 * Order Repository - N+1 Problem Solutions
 * ═══════════════════════════════════════════════════════════════
 *
 * TOPIC: N+1 Problem Deep Dive
 * ─────────────────────────────────────
 *
 * Problem Reproduction:
 *   orderRepo.findAll()  → 1 query for orders
 *   Then each order.getUser() → N queries (one per unique user)
 *
 * 3 Solutions shown below:
 * 1. JOIN FETCH (JPQL)
 * 2. @EntityGraph
 * 3. Batch Fetch (configured in application.yml)
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ❌ BAD - Triggers N+1 when accessing order.getUser()
    List<Order> findAll();

    // ✅ FIX 1: JOIN FETCH - single query with JOIN
    @Query("SELECT o FROM Order o JOIN FETCH o.user WHERE o.user.id = :userId")
    List<Order> findByUserIdWithUser(@Param("userId") Long userId);

    // ✅ FIX 2: @EntityGraph - load user and items in one query (LEFT JOIN)
    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId")
    List<Order> findByUserIdWithDetails(@Param("userId") Long userId);

    // ✅ FIX 3: Pagination with EntityGraph (avoids CartesianProduct issue with collections)
    // For paginated queries with collections, use separate queries:
    @Query(
        value = "SELECT o FROM Order o JOIN FETCH o.user",
        countQuery = "SELECT COUNT(o) FROM Order o"  // separate count query for pagination
    )
    Page<Order> findAllWithUser(Pageable pageable);

    // TOPIC: Batch Update - update multiple rows in one query (avoids N individual updates)
    @Modifying
    @Query("UPDATE Order o SET o.status = :status WHERE o.user.id = :userId AND o.status = 'PENDING'")
    int cancelPendingOrdersByUser(@Param("userId") Long userId,
                                  @Param("status") Order.OrderStatus status);

    // TOPIC: Query Optimization - aggregate query
    @Query("SELECT COUNT(o), SUM(o.totalAmount) FROM Order o WHERE o.user.id = :userId")
    Object[] getOrderStatsForUser(@Param("userId") Long userId);

    Optional<Order> findById(Long id);
}
