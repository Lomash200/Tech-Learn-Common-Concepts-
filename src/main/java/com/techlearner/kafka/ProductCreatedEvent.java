package com.techlearner.kafka;

import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent implements Serializable {
    private Long productId;
    private String name;
    private String category;
    private BigDecimal price;
    private LocalDateTime createdAt;
}
