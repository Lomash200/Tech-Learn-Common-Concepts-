package com.techlearner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * ═══════════════════════════════════════════════════════════════
 * User Entity
 * ═══════════════════════════════════════════════════════════════
 *
 * TOPIC: Hibernate Session & ORM
 * ─────────────────────────────────────
 * @Entity → Hibernate is class ko DB table se map karta hai
 * @Table  → Table name explicitly define karta hai
 *
 * Hibernate States:
 * 1. TRANSIENT  → new User() kiya, persist nahi hua
 * 2. PERSISTENT → Session ke saath associated hai (DB mein save)
 * 3. DETACHED   → Session close ho gayi, object memory mein hai par DB se disconnected
 * 4. REMOVED    → delete() called, pending deletion
 *
 * TOPIC: DB Indexing Internals
 * ─────────────────────────────────────
 * @Index annotation se PostgreSQL mein B-Tree index create hota hai.
 * Index benefits: O(log n) lookup instead of O(n) full table scan.
 * email pe index kyun? → login query: WHERE email = ? → frequently used
 *
 * TOPIC: DB Transactions & Isolation
 * ─────────────────────────────────────
 * UserService mein @Transactional dekho. Ye entity usi transaction ke
 * context mein load/save hoti hai.
 */
@Entity
@Table(name = "users", indexes = {
        // TOPIC: Indexing Internals - email frequently queried, so index it
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "user_seq")
    @SequenceGenerator(name = "user_seq",
            sequenceName = "user_sequence",
            allocationSize = 50)  // Batch allocate 50 IDs → performance optimization
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;  // BCrypt hashed

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /*
     * TOPIC: N+1 Problem Demo
     * ──────────────────────
     * FetchType.LAZY = orders tab load honge jab explicitly access karein
     * FetchType.EAGER = user ke saath IMMEDIATELY orders bhi load (N+1 ka source!)
     *
     * N+1 Problem:
     * SELECT * FROM users → 10 users
     * Phir EACH user ke liye: SELECT * FROM orders WHERE user_id = ? → 10 queries
     * Total: 1 + 10 = 11 queries! Ye N+1 problem hai.
     *
     * Fix: @EntityGraph ya JOIN FETCH in JPQL (dekho OrderRepository)
     */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Order> orders = new HashSet<>();

    public enum Role {
        USER, ADMIN
    }
}
