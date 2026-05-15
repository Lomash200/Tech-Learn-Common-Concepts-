package com.techlearner.service;

import com.techlearner.dto.Dtos.*;
import com.techlearner.entity.*;
import com.techlearner.kafka.EventProducer;
import com.techlearner.kafka.OrderPlacedEvent;
import com.techlearner.repository.OrderRepository;
import com.techlearner.repository.ProductRepository;
import com.techlearner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 * Order Service - N+1 Problem Demo + Transaction Isolation
 * ═══════════════════════════════════════════════════════════════
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EventProducer eventProducer;

    /**
     * TOPIC: N+1 Problem + @Transactional Isolation
     * ─────────────────────────────────────
     * Isolation.REPEATABLE_READ:
     * - Agar same data 2 baar read karo transaction mein → same result
     * - Phantom reads possible (new rows insert ho sakte hain)
     * - Good for: Cart checkout (price shouldn't change mid-read)
     *
     * Kab use karo:
     * READ_COMMITTED (default) → Most reads/writes
     * REPEATABLE_READ          → Checkout, balance transfer
     * SERIALIZABLE             → Financial, critical operations (slowest)
     * READ_UNCOMMITTED         → Analytics, dirty reads OK (fastest, risky)
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public OrderResponse createOrder(String userEmail, CreateOrderRequest request) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

            // Stock check karo
            if (product.getStock() < itemReq.getQuantity()) {
                throw new RuntimeException("Insufficient stock for: " + product.getName());
            }

            // Stock reduce karo (TOPIC: Optimistic Locking via @Version protects this)
            product.setStock(product.getStock() - itemReq.getQuantity());
            productRepository.save(product);

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            items.add(item);

            total = total.add(product.getPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        Order order = Order.builder()
                .user(user)
                .totalAmount(total)
                .status(Order.OrderStatus.PENDING)
                .build();

        items.forEach(item -> item.setOrder(order));
        order.setItems(items);

        Order saved = orderRepository.save(order);

        // Async Kafka event
        eventProducer.publishOrderPlaced(OrderPlacedEvent.builder()
                .orderId(saved.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .totalAmount(total)
                .placedAt(LocalDateTime.now())
                .build());

        return toOrderResponse(saved);
    }

    /**
     * ✅ GOOD: EntityGraph use karo - N+1 fix!
     * Single query with LEFT JOINs
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrdersFixed(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // JOIN FETCH → single query
        // SELECT o FROM Order o JOIN FETCH o.user JOIN FETCH o.items JOIN FETCH items.product
        List<Order> orders = orderRepository.findByUserIdWithDetails(user.getId());

        return orders.stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    /**
     * ❌ BAD: N+1 Problem DEMONSTRATION
     * Use this to SEE the problem in logs!
     * Check logs: you'll see 1 + N queries!
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrdersNPlusOneProblem(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // BAD: findAll() without JOIN FETCH
        List<Order> orders = orderRepository.findAll()
                .stream()
                .filter(o -> o.getUser().getId().equals(user.getId()))  // Each o.getUser() → 1 EXTRA QUERY!
                .collect(Collectors.toList());

        log.warn("N+1 DEMO: Check logs above - you should see multiple SELECT queries!");
        return orders.stream().map(this::toOrderResponse).collect(Collectors.toList());
    }

    /**
     * Paginated orders - TOPIC: Pagination Strategies
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersPaginated(int page, int size) {
        Page<Order> orderPage = orderRepository.findAllWithUser(
                PageRequest.of(page, size)
        );

        List<OrderResponse> responses = orderPage.getContent()
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());

        return PagedResponse.<OrderResponse>builder()
                .content(responses)
                .page(page)
                .size(size)
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .last(orderPage.isLast())
                .build();
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .username(order.getUser().getUsername())
                .items(itemResponses)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
