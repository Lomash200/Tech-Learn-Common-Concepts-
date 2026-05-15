package com.techlearner.dto;

import com.techlearner.entity.Order;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs (Data Transfer Objects) - Entity ko directly expose mat karo!
 * Reasons:
 * 1. Security: password field expose ho sakta hai
 * 2. N+1: Lazy loaded fields serialize hote waqt extra queries
 * 3. Flexibility: API contract alag ho sakta hai DB schema se
 */
public class Dtos {

    // ─── Auth DTOs ───────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Username required")
        @Size(min = 3, max = 50)
        private String username;

        @Email(message = "Valid email required")
        @NotBlank
        private String email;

        @NotBlank
        @Size(min = 6, message = "Password must be at least 6 chars")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank private String email;
        @NotBlank private String password;
    }

    @Data
    @Builder
    public static class AuthResponse {
        private String token;
        private String type = "Bearer";
        private String username;
        private String email;
        private String role;
    }

    // ─── Product DTOs ─────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductRequest {
        @NotBlank private String name;
        private String description;
        @NotNull @DecimalMin("0.01") private BigDecimal price;
        @NotBlank private String category;
        @Min(0) private Integer stock;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductResponse {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private String category;
        private Integer stock;
        private LocalDateTime createdAt;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductSummaryDto {
        private Long id;
        private String name;
        private BigDecimal price;
        private String category;
    }

    // ─── Order DTOs ───────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderRequest {
        @NotEmpty private List<OrderItemRequest> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        @NotNull private Long productId;
        @Min(1) private Integer quantity;
    }

    @Data
    @Builder
    public static class OrderResponse {
        private Long id;
        private String username;
        private List<OrderItemResponse> items;
        private BigDecimal totalAmount;
        private Order.OrderStatus status;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class OrderItemResponse {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
    }

    // ─── Pagination Wrapper ────────────────────────────────────
    @Data
    @Builder
    public static class PagedResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }

    // ─── Error Response ────────────────────────────────────────
    @Data
    @Builder
    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private LocalDateTime timestamp;
    }
}
