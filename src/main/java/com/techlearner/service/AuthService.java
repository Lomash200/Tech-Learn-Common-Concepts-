package com.techlearner.service;

import com.techlearner.dto.Dtos.*;
import com.techlearner.entity.User;
import com.techlearner.repository.UserRepository;
import com.techlearner.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ═══════════════════════════════════════════════════════════════
 * Auth Service - Registration & Login
 * ═══════════════════════════════════════════════════════════════
 *
 * TOPIC: Spring Security + JWT Flow
 * ─────────────────────────────────────
 * Complete login flow:
 * 1. POST /api/auth/login with {email, password}
 * 2. AuthService.login() calls authenticationManager.authenticate()
 * 3. AuthenticationManager → DaoAuthenticationProvider
 * 4. DaoAuthenticationProvider → UserDetailsService.loadUserByUsername()
 * 5. BCrypt password comparison
 * 6. If success → UserDetails object return
 * 7. JwtUtils.generateToken(userDetails)
 * 8. Return JWT to client
 *
 * TOPIC: IOC Container - Dependency Injection
 * ─────────────────────────────────────
 * @RequiredArgsConstructor → Lombok generates constructor
 * Spring sees constructor with all final fields → Constructor Injection!
 * Most recommended DI pattern (immutable, testable)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username taken: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))  // BCrypt hash
                .role(User.Role.USER)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", request.getEmail());

        // Generate JWT for new user
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_USER")
                .build();

        return AuthResponse.builder()
                .token(jwtUtils.generateToken(userDetails))
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Spring Security authenticate karega (password check)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtils.generateToken(userDetails);
        log.info("User logged in: {}", request.getEmail());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
