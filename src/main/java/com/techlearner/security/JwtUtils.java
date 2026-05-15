package com.techlearner.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ═══════════════════════════════════════════════════════════════
 * TOPIC: Spring Security + JWT Flow
 * ═══════════════════════════════════════════════════════════════
 *
 * JWT (JSON Web Token) kya hai?
 * ─────────────────────────────────────
 * JWT = 3 parts: Header.Payload.Signature
 *
 * Header  (Base64): {"alg":"HS256","typ":"JWT"}
 * Payload (Base64): {"sub":"user@email.com","iat":1234567,"exp":1234567,"role":"USER"}
 * Signature:        HMACSHA256(base64(header) + "." + base64(payload), secret)
 *
 * JWT Flow:
 * ─────────────────────────────────────
 * 1. User logs in → Server generates JWT
 * 2. Client stores JWT (localStorage/cookie)
 * 3. Client sends JWT in every request: "Authorization: Bearer <token>"
 * 4. Server validates signature → no DB lookup needed!
 * 5. Extract claims (user info) from token directly
 *
 * JWT vs Session:
 * ─────────────────────────────────────
 * Session: Server stores state → not scalable (sticky sessions needed)
 * JWT: Stateless → works across multiple servers (microservices friendly!)
 *
 * Security:
 * - Secret key must be strong (256+ bits for HS256)
 * - Short expiry + Refresh tokens
 * - HTTPS always!
 * - Store in httpOnly cookie (not localStorage) for production
 */
@Component
@Slf4j
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * JWT generate karo from UserDetails
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Custom claims add kar sakte hain (role, permissions etc.)
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> extraClaims, String subject) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)                           // "sub" claim
                .setIssuedAt(new Date())                       // "iat" claim
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // "exp" claim
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // HMAC-SHA256 signature
                .compact();
    }

    /**
     * Token validate karo:
     * 1. Signature valid hai?
     * 2. Expired nahi hai?
     * 3. Username match karta hai?
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // Signature verify karta hai + claims return karta hai
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Secret key ko HMAC-SHA key mein convert karo.
     * Key must be at least 256 bits for HS256.
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes())
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
