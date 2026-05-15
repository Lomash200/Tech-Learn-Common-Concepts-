package com.techlearner.kafka;

import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent implements Serializable {
    private Long orderId;
    private Long userId;
    private String username;
    private BigDecimal totalAmount;
    private LocalDateTime placedAt;
}
