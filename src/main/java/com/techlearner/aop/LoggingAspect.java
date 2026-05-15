package com.techlearner.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * ═══════════════════════════════════════════════════════════════
 * TOPIC: Spring AOP Internals
 * ═══════════════════════════════════════════════════════════════
 *
 * AOP = Aspect-Oriented Programming
 * Cross-cutting concerns ko alag rakhne ke liye (logging, security, transactions).
 *
 * Key AOP Concepts:
 * ─────────────────────────────────────
 * 1. Aspect     → AOP logic ka container (@Aspect class)
 * 2. Advice     → What to do (@Before, @After, @Around, @AfterReturning, @AfterThrowing)
 * 3. Pointcut   → Where to apply (expression defining which methods)
 * 4. JoinPoint  → Method execution ka specific point
 * 5. Weaving    → Aspect ko target object ke saath combine karna
 *
 * How Spring AOP Works Internally:
 * ─────────────────────────────────────
 * Spring AOP = PROXY BASED (NOT bytecode instrumentation like AspectJ)
 *
 * Jab @Transactional ya @Cacheable ho:
 * 1. BeanPostProcessor (AbstractAutoProxyCreator) detect karta hai
 * 2. JDK Dynamic Proxy create hota hai (if interface) OR
 *    CGLIB Proxy (if concrete class - subclass banta hai)
 * 3. Proxy calls interceptor chain before/after real method
 *
 * THIS IS WHY: self-invocation mein @Transactional kaam nahi karta!
 * (this.method() proxy bypass karta hai)
 *
 * Pointcut Expression Syntax:
 * ─────────────────────────────────────
 * execution(modifiers? return-type declaring-type? method-name(params) throws?)
 * execution(* com.techlearner.service.*.*(..)) → all methods in service package
 * @annotation(com.techlearner.aop.LogExecution) → methods with custom annotation
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Pointcut: Service layer ke saare methods
     * ".." means any number of parameters
     */
    @Pointcut("execution(* com.techlearner.service.*.*(..))")
    public void serviceLayerPointcut() {}

    /**
     * Pointcut: Repository layer
     */
    @Pointcut("execution(* com.techlearner.repository.*.*(..))")
    public void repositoryLayerPointcut() {}

    /**
     * @Before - Method call se PEHLE execute
     * Use case: Input validation logging, security checks
     */
    @Before("serviceLayerPointcut()")
    public void logBeforeServiceCall(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        log.debug("→ Calling: {} with args: {}", methodName, Arrays.toString(args));
    }

    /**
     * @Around - Method ke AROUND (before + after)
     * Most powerful advice - can modify args, return value, or skip the method
     * Use case: Performance monitoring, caching, retry logic
     *
     * ProceedingJoinPoint → actual method call control deta hai
     * proceed() → actual method call karo
     */
    @Around("serviceLayerPointcut()")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();  // ← Actual method call!
            long elapsed = System.currentTimeMillis() - start;

            if (elapsed > 100) {
                log.warn("⚠️  SLOW METHOD: {} took {}ms", methodName, elapsed);
            } else {
                log.debug("✓ {} completed in {}ms", methodName, elapsed);
            }
            return result;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("✗ {} FAILED after {}ms: {}", methodName, elapsed, e.getMessage());
            throw e;
        }
    }

    /**
     * @AfterReturning - Method successfully return karne ke BAAD
     * Use case: Audit logging, notifications
     */
    @AfterReturning(pointcut = "serviceLayerPointcut()", returning = "result")
    public void logAfterReturn(JoinPoint joinPoint, Object result) {
        log.debug("← Returned from: {} → {}",
                joinPoint.getSignature().getName(),
                result != null ? result.getClass().getSimpleName() : "null");
    }

    /**
     * @AfterThrowing - Exception throw hone pe
     * Use case: Error tracking, alerting
     */
    @AfterThrowing(pointcut = "serviceLayerPointcut()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        log.error("✗ Exception in {}: {}",
                joinPoint.getSignature().toShortString(),
                exception.getMessage());
    }
}
