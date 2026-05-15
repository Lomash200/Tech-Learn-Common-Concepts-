package com.techlearner.config;

import com.techlearner.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ═══════════════════════════════════════════════════════════════
 * TOPIC: Spring Security + JWT Flow - Security Configuration
 * ═══════════════════════════════════════════════════════════════
 *
 * TOPIC: IOC Container & Bean Lifecycle
 * ─────────────────────────────────────
 * @Configuration = Spring ko batao ki ye class beans define karti hai
 * @Bean methods = Spring in objects ko manage karega
 *
 * SecurityFilterChain:
 * ─────────────────────────────────────
 * Spring Security ek filter chain hai. Har request in filters se guzarti hai:
 * DisableEncodeUrlFilter
 * WebAsyncManagerIntegrationFilter
 * SecurityContextHolderFilter
 * HeaderWriterFilter
 * CsrfFilter
 * LogoutFilter
 * UsernamePasswordAuthenticationFilter
 * → JwtAuthFilter (hum add kar rahe hain)
 * BasicAuthenticationFilter
 * RequestCacheAwareFilter
 * SessionManagementFilter
 * ExceptionTranslationFilter
 * AuthorizationFilter
 *
 * @EnableMethodSecurity:
 * ─────────────────────────────────────
 * Method-level security enable karta hai:
 * @PreAuthorize("hasRole('ADMIN')")  → method call se pehle check
 * @PostAuthorize("returnObject.owner == authentication.name")  → after return
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize, @PostAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())  // REST API mein CSRF ki zarurat nahi (stateless)

                .sessionManagement(session ->
                        // STATELESS: No HTTP session, every request JWT se authenticate hoga
                        // TOPIC: Thread Safety - No shared session state between threads
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no auth needed)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/internals/**").permitAll()

                        // Admin only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // All other endpoints need authentication
                        .anyRequest().authenticated()
                )

                // JWT filter ko UsernamePasswordAuthenticationFilter se PEHLE add karo
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    /**
     * AuthenticationProvider:
     * UserDetailsService se user load karta hai aur password verify karta hai.
     * DaoAuthenticationProvider = DB-based authentication.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * AuthenticationManager:
     * Authentication process ko orchestrate karta hai.
     * Login endpoint mein use hoga.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCryptPasswordEncoder:
     * - One-way hashing (cannot decrypt)
     * - Adaptive cost factor (slow by design)
     * - Auto-generates salt (same password → different hash every time!)
     * - Work factor default: 10 rounds of hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // 12 = strength/cost factor
    }
}
