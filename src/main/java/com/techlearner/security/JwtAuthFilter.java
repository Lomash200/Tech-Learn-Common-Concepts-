package com.techlearner.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ═══════════════════════════════════════════════════════════════
 * TOPIC: Spring Security + JWT Flow - JWT Filter
 * ═══════════════════════════════════════════════════════════════
 *
 * Request Flow (TOPIC: DispatcherServlet Request Flow):
 * ─────────────────────────────────────────────────────
 * HTTP Request
 *     ↓
 * Filter Chain (Security Filters)
 *     ↓  ← JwtAuthFilter is here
 * DispatcherServlet
 *     ↓
 * HandlerMapping (find right controller)
 *     ↓
 * HandlerAdapter (call controller method)
 *     ↓
 * Controller → Service → Repository
 *     ↓
 * ViewResolver (for MVC) or MessageConverter (for REST)
 *     ↓
 * HTTP Response
 *
 * OncePerRequestFilter → Ensures filter runs exactly ONCE per request
 * (important for forwarded requests)
 *
 * SecurityContextHolder:
 * ─────────────────────────────────────
 * Ek ThreadLocal wrapper hai jo current thread ka authentication store karta hai.
 * TOPIC: ThreadLocal - Each thread apna SecurityContext maintain karta hai.
 * Thread pool mein threads reuse hote hain → isliye SecurityContext clear karna zaroori hai.
 * Spring Security automatically clear karta hai after response.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);

            if (jwt != null) {
                String username = jwtUtils.extractUsername(jwt);

                // Only authenticate if not already authenticated
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // DB se user load karo (UserDetailsService)
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtUtils.validateToken(jwt, userDetails)) {
                        // Authentication object create karo
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,  // credentials (password) - not needed after validation
                                        userDetails.getAuthorities()
                                );
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        // TOPIC: ThreadLocal - SecurityContext is thread-local
                        // Is thread ke liye authentication set karo
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.debug("JWT validated for user: {}", username);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        // Next filter ko pass karo (chain continue)
        filterChain.doFilter(request, response);
    }

    /**
     * "Authorization: Bearer <token>" header se JWT extract karo
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);  // "Bearer " (7 chars) ke baad
        }
        return null;
    }
}
