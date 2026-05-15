package com.techlearner.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * ═══════════════════════════════════════════════════════════════
 * TOPIC: Message Queues (Kafka)
 * ═══════════════════════════════════════════════════════════════
 *
 * Kafka kya hai?
 * ─────────────────────────────────────
 * Distributed event streaming platform.
 * Messages → Topics mein publish hote hain.
 * Consumers → Topics se messages read karte hain.
 *
 * Key Concepts:
 * ─────────────────────────────────────
 * Topic      → Messages ka category/channel (like a queue, but persistent)
 * Partition  → Topic horizontally split hota hai → scalability
 * Broker     → Kafka server (1+ brokers = Kafka cluster)
 * Producer   → Messages bhejta hai
 * Consumer   → Messages padhta hai
 * Consumer Group → Multiple consumers ek group mein → parallel processing
 * Offset     → Consumer ne kitna padha, ye track karta hai
 *
 * Kafka vs Traditional MQ (RabbitMQ):
 * ─────────────────────────────────────
 * Kafka:    Messages disk pe store (days/weeks), replay possible, high throughput
 * RabbitMQ: Message consume ho → delete ho jata hai, complex routing, lower latency
 *
 * Use Kafka for: Event sourcing, audit logs, stream processing, high volume
 * Use RabbitMQ for: Task queues, RPC, complex routing, guaranteed delivery
 *
 * Delivery Guarantees:
 * ─────────────────────────────────────
 * At-most-once  → Message kabhi duplicate nahi, par miss ho sakta hai
 * At-least-once → Message kabhi miss nahi, par duplicate ho sakta hai
 * Exactly-once  → Perfect, par complex (Kafka Transactions needed)
 *
 * Partition Strategy:
 * ─────────────────────────────────────
 * Default: Round-robin
 * With key: hash(key) % numPartitions → Same key always same partition
 * → Ordering guaranteed within a partition for same key
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    // Topic names as constants
    public static final String PRODUCT_CREATED_TOPIC = "product-created";
    public static final String ORDER_PLACED_TOPIC = "order-placed";
    public static final String NOTIFICATION_TOPIC = "notifications";

    /**
     * Topic creation:
     * partitions(3) → 3 parallel partitions → 3x throughput potential
     * replicas(1) → 1 replica (use 3 in production for fault tolerance)
     */
    @Bean
    public NewTopic productCreatedTopic() {
        return TopicBuilder.name(PRODUCT_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name(ORDER_PLACED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name(NOTIFICATION_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
