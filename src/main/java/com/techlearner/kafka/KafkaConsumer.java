package com.techlearner.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * ═══════════════════════════════════════════════════════════════
 * TOPIC: Message Queues (Kafka) - Consumer
 * ═══════════════════════════════════════════════════════════════
 *
 * @KafkaListener kaise kaam karta hai:
 * ─────────────────────────────────────
 * 1. Application start pe → Consumer thread start hota hai
 * 2. Consumer group "techlearner-group" se Kafka broker se connect hota hai
 * 3. Partition assignment hoti hai (agar multiple consumers hain group mein)
 * 4. Poll loop: broker se messages batch mein fetch hote hain
 * 5. Deserialize: JSON → Java object (Jackson)
 * 6. Listener method invoke hoti hai
 * 7. Success → offset commit (auto/manual)
 * 8. Exception → retry policy apply hoti hai
 *
 * Consumer Group Scaling:
 * ─────────────────────────────────────
 * Topic has 3 partitions:
 * - 1 instance → 1 consumer, reads all 3 partitions
 * - 3 instances → 3 consumers, 1 partition each (max parallelism!)
 * - 4 instances → 1 consumer idle (more consumers than partitions = wasteful)
 *
 * Idempotency:
 * ─────────────────────────────────────
 * At-least-once delivery = same message can come twice!
 * Solution: Store processed event IDs in Redis/DB
 * if (redisTemplate.opsForSet().isMember("processed_events", event.getId())) return;
 */
@Service
@Slf4j
public class KafkaConsumer {

    @KafkaListener(
            topics = KafkaConfig.PRODUCT_CREATED_TOPIC,
            groupId = "techlearner-group"
    )
    public void consumeProductCreated(
            @Payload ProductCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("📥 [Kafka] ProductCreated received: id={}, name='{}', partition={}, offset={}",
                event.getProductId(), event.getName(), partition, offset);

        // Yahan real business logic aayegi:
        // updateSearchIndex(event);      → Elasticsearch
        // notifyAnalytics(event);        → Analytics service
        // warmUpCache(event);            → Redis cache pre-load
    }

    @KafkaListener(
            topics = KafkaConfig.ORDER_PLACED_TOPIC,
            groupId = "techlearner-group"
    )
    public void consumeOrderPlaced(@Payload OrderPlacedEvent event) {
        log.info("📥 [Kafka] OrderPlaced received: orderId={}, userId={}, amount={}",
                event.getOrderId(), event.getUserId(), event.getTotalAmount());

        // Yahan real business logic:
        // sendConfirmationEmail(event);  → Email service
        // updateInventory(event);        → Inventory service
        // recordForAnalytics(event);     → Analytics
    }
}
