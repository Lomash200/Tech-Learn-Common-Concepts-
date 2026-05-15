package com.techlearner.aop;

import java.lang.annotation.*;

/**
 * Custom annotation for rate limiting.
 * AOP aspect will intercept methods annotated with this.
 *
 * TOPIC: Spring AOP - Custom Annotations as Pointcuts
 * ─────────────────────────────────────
 * @annotation(com.techlearner.aop.RateLimit) pointcut expression
 * matches ANY method in ANY class that has @RateLimit.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int requestsPerMinute() default 60;
    String description() default "";
}
