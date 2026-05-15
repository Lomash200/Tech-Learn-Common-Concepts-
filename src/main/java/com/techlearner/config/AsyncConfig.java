package com.techlearner.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ═══════════════════════════════════════════════════════════════
 * TOPIC: Thread Pool + Async Processing
 * ═══════════════════════════════════════════════════════════════
 *
 * Thread Pool kya hai?
 * ─────────────────────────────────────
 * Pre-created threads ka pool. Har nayi task pe naya thread banane ki
 * jagah, existing thread reuse karo. Benefits:
 * 1. Thread creation overhead remove
 * 2. Thread count bounded (memory control)
 * 3. Task queuing
 *
 * ThreadPoolExecutor Parameters:
 * ─────────────────────────────────────
 * corePoolSize  → Always alive threads (minimum)
 * maxPoolSize   → Maximum threads (when queue full)
 * queueCapacity → Tasks wait karne ki jagah (when core threads busy)
 *
 * Flow:
 * Task submit karo
 * → Core threads free hain? → Core thread use karo
 * → Core threads busy? → Queue mein daalo
 * → Queue full? → New thread create karo (up to max)
 * → Max threads bhi busy + queue full? → RejectedExecutionHandler!
 *
 * TOPIC: Threads Internals
 * ─────────────────────────────────────
 * Java Thread States:
 * NEW → RUNNABLE → RUNNING → BLOCKED/WAITING/TIMED_WAITING → TERMINATED
 *
 * @Async kaise kaam karta hai:
 * ─────────────────────────────────────
 * 1. @Async method call hoti hai
 * 2. AOP Proxy intercept karta hai (TOPIC: Spring AOP!)
 * 3. Method run hoti hai Thread Pool ke ek thread pe
 * 4. Caller thread immediately return ho jata hai
 * 5. Result chahiye? CompletableFuture use karo
 */
@Configuration
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // TOPIC: Thread Pool
        executor.setCorePoolSize(4);         // Always-alive threads
        executor.setMaxPoolSize(10);          // Peak capacity
        executor.setQueueCapacity(100);       // Waiting room for tasks
        executor.setThreadNamePrefix("TechLearner-Async-");  // Thread naming (visible in thread dump)
        executor.setKeepAliveSeconds(60);     // Idle extra threads cleanup

        /*
         * Rejection Policy: Queue bhi full ho jaaye to kya karo?
         * Options:
         * AbortPolicy          → RejectedExecutionException throw (default)
         * CallerRunsPolicy     → Caller thread mein run karo (slows down producer - backpressure!)
         * DiscardPolicy        → Task silently drop karo
         * DiscardOldestPolicy  → Queue ka oldest task drop karo, naya add karo
         */
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        log.info("Async Thread Pool initialized: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }
}
