package com.techlearner.controller;

import com.techlearner.dto.Dtos.*;
import com.techlearner.service.JvmInternalsService;
import com.techlearner.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Order Controller
 * TOPIC: N+1 Problem Demo endpoints
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        String email = authentication.getName();  // JWT se email
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(email, request));
    }

    /**
     * ✅ GET /api/orders/fixed → Uses JOIN FETCH (No N+1)
     * Check logs: only 1-2 queries
     */
    @GetMapping("/fixed")
    public ResponseEntity<List<OrderResponse>> getMyOrdersFixed(Authentication authentication) {
        return ResponseEntity.ok(orderService.getUserOrdersFixed(authentication.getName()));
    }

    /**
     * ❌ GET /api/orders/nplusone → Demonstrates N+1 problem
     * Check logs: you'll see many SELECT queries!
     * Compare query count with /fixed endpoint.
     */
    @GetMapping("/nplusone")
    public ResponseEntity<List<OrderResponse>> getMyOrdersNPlusOne(Authentication authentication) {
        return ResponseEntity.ok(
                orderService.getUserOrdersNPlusOneProblem(authentication.getName())
        );
    }

    /**
     * GET /api/orders/paginated?page=0&size=5
     * TOPIC: Pagination with JOIN FETCH
     */
    @GetMapping("/paginated")
    public ResponseEntity<PagedResponse<OrderResponse>> getOrdersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(orderService.getOrdersPaginated(page, size));
    }
}

/**
 * ═══════════════════════════════════════════════════════════════
 * JVM Internals Controller
 * ═══════════════════════════════════════════════════════════════
 * All JVM topics accessible via REST endpoints!
 */
@RestController
@RequestMapping("/api/internals")
@RequiredArgsConstructor
class JvmController {

    private final JvmInternalsService jvmInternalsService;

    /**
     * GET /api/internals/jvm
     * TOPIC: JVM Memory Model - live heap, metaspace, GC info
     */
    @GetMapping("/jvm")
    public ResponseEntity<Map<String, Object>> getJvmMemory() {
        return ResponseEntity.ok(jvmInternalsService.getJvmMemoryInfo());
    }

    /**
     * GET /api/internals/gc
     * TOPIC: Garbage Collection - GC stats, thread info
     */
    @GetMapping("/gc")
    public ResponseEntity<Map<String, Object>> getGcStats() {
        return ResponseEntity.ok(jvmInternalsService.getGcStats());
    }

    /**
     * GET /api/internals/threads
     * TOPIC: Threads Internals - thread states, deadlock detection
     */
    @GetMapping("/threads")
    public ResponseEntity<Map<String, Object>> getThreadInfo() {
        return ResponseEntity.ok(jvmInternalsService.getThreadInfo());
    }

    /**
     * GET /api/internals/hashmap
     * TOPIC: HashMap Internals - collision, resize, tree conversion
     */
    @GetMapping("/hashmap")
    public ResponseEntity<Map<String, Object>> getHashMapInfo() {
        return ResponseEntity.ok(jvmInternalsService.demonstrateHashMapInternals());
    }

    /**
     * GET /api/internals/concurrent-hashmap
     * TOPIC: ConcurrentHashMap - thread-safe operations demo
     */
    @GetMapping("/concurrent-hashmap")
    public ResponseEntity<Map<String, Object>> getConcurrentHashMapInfo() {
        return ResponseEntity.ok(jvmInternalsService.demonstrateConcurrentHashMap());
    }

    /**
     * GET /api/internals/architecture
     * TOPIC: Backend Architecture concepts summary
     */
    @GetMapping("/architecture")
    public ResponseEntity<Map<String, Object>> getArchitectureInfo() {
        return ResponseEntity.ok(Map.of(
                "loadBalancer", Map.of(
                        "what", "Distributes incoming traffic across multiple server instances",
                        "types", List.of("Round Robin", "Least Connections", "IP Hash", "Weighted"),
                        "tools", List.of("Nginx", "HAProxy", "AWS ALB/NLB", "Spring Cloud LoadBalancer"),
                        "spring_demo", "Spring Cloud @LoadBalanced RestTemplate or WebClient"
                ),
                "apiGateway", Map.of(
                        "what", "Single entry point for all client requests",
                        "responsibilities", List.of(
                                "Routing", "Auth/JWT verification", "Rate Limiting",
                                "SSL Termination", "Request/Response transformation",
                                "Circuit Breaking", "Logging"
                        ),
                        "tools", List.of("Spring Cloud Gateway", "Kong", "AWS API Gateway", "Nginx")
                ),
                "microservices", Map.of(
                        "what", "Application split into small, independent services",
                        "patterns", List.of(
                                "Service Discovery (Eureka)",
                                "Circuit Breaker (Resilience4j)",
                                "Saga Pattern (distributed transactions)",
                                "CQRS (Command Query Responsibility Segregation)",
                                "Event Sourcing"
                        ),
                        "communication", List.of("REST (sync)", "gRPC (sync, fast)", "Kafka (async, decoupled)")
                ),
                "caching_strategies", Map.of(
                        "cache_aside", "App manages cache (our Redis impl)",
                        "write_through", "@CachePut on every write",
                        "write_behind", "Async DB write after cache update",
                        "read_through", "Cache loads from DB automatically"
                ),
                "rateLimiting", Map.of(
                        "implemented", "Fixed Window with Redis (see RateLimitingAspect)",
                        "other_algorithms", List.of("Token Bucket", "Leaky Bucket", "Sliding Window"),
                        "production_tools", List.of("Bucket4j + Redis", "Resilience4j", "API Gateway")
                )
        ));
    }
}
