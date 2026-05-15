package com.techlearner.controller;

import com.techlearner.aop.RateLimit;
import com.techlearner.dto.Dtos.*;
import com.techlearner.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ═══════════════════════════════════════════════════════════════
 * TOPIC: DispatcherServlet Request Flow
 * ═══════════════════════════════════════════════════════════════
 *
 * Request journey:
 * 1. HTTP Request → Tomcat (Embedded Server)
 * 2. Tomcat → Spring's DispatcherServlet (Front Controller Pattern)
 * 3. DispatcherServlet → HandlerMapping (URL → Controller Method)
 *    RequestMappingHandlerMapping: @RequestMapping annotations scan karta hai
 * 4. HandlerMapping returns HandlerExecutionChain (handler + interceptors)
 * 5. HandlerAdapter → Controller method call karta hai
 *    (RequestMappingHandlerAdapter for @Controller/@RestController)
 * 6. @RequestBody → HttpMessageConverter (Jackson: JSON → Java object)
 * 7. Controller method execute hoti hai
 * 8. Return value → @ResponseBody → Jackson: Java object → JSON
 * 9. Response → Tomcat → Client
 *
 * @RestController = @Controller + @ResponseBody
 * @ResponseBody means: return value ko HTTP response body mein daalo (JSON)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @RateLimit(requestsPerMinute = 10, description = "Prevent registration spam")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @RateLimit(requestsPerMinute = 20, description = "Prevent brute force")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
