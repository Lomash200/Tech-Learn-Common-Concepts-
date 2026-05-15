package com.techlearner.repository;

import com.techlearner.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * ═══════════════════════════════════════════════════════════════
 * Product Repository
 * ═══════════════════════════════════════════════════════════════
 *
 * TOPIC: Pagination Strategies
 * ─────────────────────────────────────
 * Spring Data ka Pageable interface use karta hai:
 * PageRequest.of(page, size, Sort.by("price").ascending())
 *
 * Internally generates:
 * SELECT * FROM products ORDER BY price ASC LIMIT 10 OFFSET 0
 *
 * TOPIC: Query Optimization
 * ─────────────────────────────────────
 * @Query annotation = custom JPQL (HQL) queries.
 * JPQL: Object-oriented query (entities pe), SQL: table pe.
 *
 * Native Query: direct SQL for complex queries PostgreSQL specific features use karne ke liye.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // TOPIC: Indexing - category column pe index hai, ye query fast hogi
    Page<Product> findByCategory(String category, Pageable pageable);

    // TOPIC: Query Optimization - JPQL with parameter binding
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.price BETWEEN :minPrice AND :maxPrice ORDER BY p.price ASC")
    List<Product> findByCategoryAndPriceRange(
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);

    // TOPIC: Full Text Search - PostgreSQL native query
    @Query(value = "SELECT * FROM products WHERE to_tsvector('english', name || ' ' || COALESCE(description, '')) @@ plainto_tsquery('english', :searchTerm)",
            nativeQuery = true)
    List<Product> fullTextSearch(@Param("searchTerm") String searchTerm);

    // TOPIC: Cursor-based Pagination (Keyset Pagination) - faster than OFFSET for large datasets
    @Query("SELECT p FROM Product p WHERE p.id > :lastId ORDER BY p.id ASC")
    List<Product> findProductsAfter(@Param("lastId") Long lastId, Pageable pageable);

    // Readonly hint - tells Hibernate no dirty checking needed → faster reads
    @QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value = "true"))
    @Query("SELECT p FROM Product p WHERE p.stock > 0")
    List<Product> findAvailableProductsReadOnly();

    Optional<Product> findByName(String name);

    // TOPIC: Projection - fetch only required columns (avoids fetching large TEXT fields)
//    @Query("SELECT new com.techlearner.dto.ProductSummaryDto(p.id, p.name, p.price, p.category) FROM Product p")
//    Page<ProductSummaryDto> findAllSummaries(Pageable pageable);
    @Query("SELECT p.id as id, p.name as name, p.price as price, p.category as category FROM Product p")
    Page<ProductSummaryDto> findAllSummaries(Pageable pageable);

    // Inner interface for projection
    interface ProductSummaryDto {
        Long getId();
        String getName();
        BigDecimal getPrice();
        String getCategory();
    }
}
