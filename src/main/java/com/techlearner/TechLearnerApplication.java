package com.techlearner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ═══════════════════════════════════════════════════════════════
 * TechLearner Application - Main Entry Point
 * ═══════════════════════════════════════════════════════════════
 *
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 *
 * TOPIC: IOC Container & Bean Lifecycle
 * ─────────────────────────────────────
 * Jab ye class load hoti hai, Spring ka ApplicationContext create hota hai.
 * ApplicationContext = IOC Container.
 *
 * Bean Lifecycle:
 * 1. BeanDefinition scan (via @Component, @Service, @Repository etc.)
 * 2. BeanDefinition registered in BeanFactory
 * 3. BeanFactory instantiates beans (constructor call)
 * 4. Dependency Injection (@Autowired fields/constructor inject karta hai)
 * 5. @PostConstruct method called
 * 6. Bean ready for use
 * 7. App shutdown → @PreDestroy called
 *
 * TOPIC: Dependency Injection Internals
 * ─────────────────────────────────────
 * Spring 3 types of DI support karta hai:
 * 1. Constructor Injection (RECOMMENDED - immutable, testable)
 * 2. Setter Injection
 * 3. Field Injection (@Autowired directly on field - avoid in production)
 *
 * Internally: Spring uses Java Reflection API to inject dependencies.
 * BeanPostProcessor interface ke through proxy beans bhi create hote hain (AOP ke liye).
 */
@SpringBootApplication
@EnableCaching          // TOPIC: Redis Caching - Enables @Cacheable, @CacheEvict etc.
@EnableAsync            // TOPIC: Async Processing - Enables @Async methods
@EnableScheduling       // Enables @Scheduled tasks
public class TechLearnerApplication {

    public static void main(String[] args) {
        /*
         * SpringApplication.run() kya karta hai internally:
         * 1. Creates SpringApplication instance
         * 2. Determines application type (SERVLET/REACTIVE/NONE)
         * 3. Loads ApplicationContext (AnnotationConfigServletWebServerApplicationContext)
         * 4. Registers all beans
         * 5. Starts embedded Tomcat server
         * 6. DispatcherServlet register hota hai (TOPIC: DispatcherServlet)
         */
        SpringApplication app = new SpringApplication(TechLearnerApplication.class);
        app.run(args);

        System.out.println("""
                ╔══════════════════════════════════════════════════════╗
                ║         TechLearner Application Started!             ║
                ║                                                      ║
                ║  Endpoints:                                          ║
                ║  POST /api/auth/register   → Register user           ║
                ║  POST /api/auth/login      → Get JWT token           ║
                ║  GET  /api/products        → List (Cached + Paged)   ║
                ║  POST /api/products        → Create (Kafka event)    ║
                ║  GET  /api/orders          → N+1 Demo                ║
                ║  GET  /api/internals/jvm   → JVM Memory Info         ║
                ║  GET  /api/internals/gc    → GC Stats                ║
                ║  GET  /api/internals/threads → Thread Pool Info      ║
                ║  GET  /actuator/threaddump → Thread Dump             ║
                ║  GET  /actuator/heapdump   → Heap Dump               ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }
}
