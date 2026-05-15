package com.techlearner.security;

import com.techlearner.entity.User;
import com.techlearner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TOPIC: Spring Security - UserDetailsService
 * ─────────────────────────────────────────────
 * Spring Security iska use karta hai user ko DB se load karne ke liye.
 * JwtAuthFilter mein: userDetailsService.loadUserByUsername(email)
 *
 * TOPIC: @Transactional Internals
 * ─────────────────────────────────────────────
 * @Transactional(readOnly=true):
 * - Read-only transaction open hota hai
 * - Hibernate dirty checking disable → performance improve
 * - DB level pe read-only hints diye ja sakte hain
 * - Connection pool se connection liya jata hai
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Spring Security ka UserDetails object return karo
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())  // BCrypt hashed password
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}
