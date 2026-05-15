package com.techlearner.config;

import com.techlearner.dto.Dtos.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 * Global Exception Handler
 * ═══════════════════════════════════════════════════════════════
 *
 * TOPIC: DispatcherServlet Request Flow
 * ─────────────────────────────────────
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * DispatcherServlet exception resolve karta hai via HandlerExceptionResolver.
 * ExceptionHandlerExceptionResolver → @ExceptionHandler methods dhundta hai.
 *
 * AOP ka internal use: @ControllerAdvice ek AOP concept hai!
 * Sare controllers pe transparently apply hota hai.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("RuntimeException: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.builder()
                .status(401)
                .error("Unauthorized")
                .message("Invalid email or password")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.builder()
                .status(403)
                .error("Forbidden")
                .message("You don't have permission to access this resource")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(ErrorResponse.builder()
                .status(e.getStatusCode().value())
                .error(e.getStatusCode().toString())
                .message(e.getReason())
                .timestamp(LocalDateTime.now())
                .build());
    }

    // Validation errors (@Valid annotation fail hone pe)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        field -> field.getDefaultMessage() != null ? field.getDefaultMessage() : "Invalid value"
                ));

        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "Validation Failed",
                "fieldErrors", errors,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
