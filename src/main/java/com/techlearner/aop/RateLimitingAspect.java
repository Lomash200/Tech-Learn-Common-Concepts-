package com.techlearner.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/**
 * ═══════════════════════════════════════════════════════════════
 * TOPIC: Rate Limiting (via AOP + Redis)
 * ═══════════════════════════════════════════════════════════════
 *
 * Rate Limiting kya hai?
 * ─────────────────────────────────────
 * Client ko limit karna: ek specific time window mein kitne requests
 * kar sakta hai. Prevents: DDoS, API abuse, resource exhaustion.
 *
 * Algorithms:
 * 1. Fixed Window Counter  → Simple, but burst at window boundary
 * 2. Sliding Window Log   → Accurate, but memory heavy
 * 3. Token Bucket         → Smooth, allows burst within limit
 * 4. Leaky Bucket         → Constant output rate
 *
 * Hum Fixed Window + Redis implement kar rahe hain:
 * ─────────────────────────────────────
 * Key: "rate_limit:{IP}:{minute}"
 * Value: request count
 * TTL: 60 seconds (auto-expires)
 *
 * Redis INCR command atomic hai → thread-safe!
 * Distributed system mein bhi kaam karta hai (multiple instances).
 *
 * Production alternatives: Bucket4j, Resilience4j, API Gateway rate limiting
 */
@Aspect
@Component
@Slf4j
public class RateLimitingAspect {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * @RateLimit annotation wale controller methods pe apply hoga
     * Custom annotation se fine-grained control milta hai
     */
    @Around("@annotation(com.techlearner.aop.RateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        String clientIp = getClientIp();
        String currentMinute = String.valueOf(System.currentTimeMillis() / 60000);
        String key = "rate_limit:" + clientIp + ":" + currentMinute;

        // Redis INCR - atomic increment (thread-safe, distributed-safe)
        Long requestCount = redisTemplate.opsForValue().increment(key);

        if (requestCount == 1) {
            // First request in this window → set TTL
            redisTemplate.expire(key, Duration.ofSeconds(60));
        }

        log.debug("Rate limit check for IP {}: {}/{} requests", clientIp, requestCount, MAX_REQUESTS_PER_MINUTE);

        if (requestCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit EXCEEDED for IP: {} → {} requests in 1 minute", clientIp, requestCount);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded. Max " + MAX_REQUESTS_PER_MINUTE + " requests per minute."
            );
        }

        return joinPoint.proceed();
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();

            // X-Forwarded-For header check (behind load balancer)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
