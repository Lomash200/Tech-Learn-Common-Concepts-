package com.techlearner.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * TOPIC: Message Queues (Kafka) - Producer
 *
 * Kafka Internals:
 * - Producer sends to Topic Partition based on key hash
 * - key = productId → same product always same partition → ordering guaranteed
 * - Async send returns CompletableFuture → non-blocking!
 * - acks=all (config) → wait for all replicas → durability
 *
 * Delivery Guarantees:
 * At-most-once  → acks=0 (fire and forget)
 * At-least-once → acks=all + retries (our config)
 * Exactly-once  → Kafka Transactions (complex, use only when needed)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishProductCreated(ProductCreatedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(
                        KafkaConfig.PRODUCT_CREATED_TOPIC,
                        event.getProductId().toString(),
                        event
                );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✓ Published ProductCreated: productId={}, partition={}, offset={}",
                        event.getProductId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("✗ Failed to publish ProductCreated: {}", ex.getMessage());
            }
        });
    }

    public void publishOrderPlaced(OrderPlacedEvent event) {
        kafkaTemplate.send(KafkaConfig.ORDER_PLACED_TOPIC, event.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✓ Published OrderPlaced: orderId={}", event.getOrderId());
                    } else {
                        log.error("✗ Failed to publish OrderPlaced: {}", ex.getMessage());
                    }
                });
    }
}
