package com.techlearner.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════
 * TOPIC: Caching (Redis)
 * ═══════════════════════════════════════════════════════════════
 *
 * Redis kya hai?
 * ─────────────────────────────────────
 * In-memory key-value store. RAM mein data store karta hai.
 * Extremely fast: microsecond latency vs DB ki millisecond latency.
 *
 * Redis Data Structures:
 * String, List, Set, Sorted Set, Hash, Stream, HyperLogLog, Bitmap
 *
 * Caching Patterns:
 * ─────────────────────────────────────
 * 1. Cache-Aside (Lazy Loading):
 *    - Check cache → miss → load from DB → store in cache → return
 *    - @Cacheable karne se automatically hota hai!
 *
 * 2. Write-Through:
 *    - Every write → DB + cache dono update
 *    - @CachePut use karo
 *
 * 3. Write-Behind (Write-Back):
 *    - Write to cache first, DB update later (async)
 *    - Higher throughput, risk of data loss
 *
 * 4. Read-Through:
 *    - Cache automatically DB se load karta hai (cache handles it)
 *
 * Cache Eviction Policies (maxmemory-policy in Redis config):
 * ─────────────────────────────────────
 * noeviction   → Error if memory full
 * allkeys-lru  → LRU eviction from all keys
 * volatile-lru → LRU eviction from keys with TTL only
 * allkeys-lfu  → LFU (Least Frequently Used)
 * allkeys-random → Random eviction
 *
 * TTL (Time To Live):
 * ─────────────────────────────────────
 * Har cache entry ek TTL ke baad expire hoti hai.
 * Stale data avoid hota hai.
 * Different caches ke liye different TTL set karo.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key: String serializer (human-readable keys)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value: JSON serializer (objects ko JSON mein store karo)
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // Default config: 60 seconds TTL
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(createJsonSerializer())
                );

        // Per-cache custom TTL (different caches ko different TTL dete hain)
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Products cache: 5 minutes (product data rarely changes)
        cacheConfigs.put("products", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // User cache: 10 minutes
        cacheConfigs.put("users", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Rate limit counters: 1 minute (synced with rate limiting window)
        cacheConfigs.put("rate-limits", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Type info include karo taaki deserialization kaam kare
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
