package com.umb.apps.payment.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    // Must be at least 32 characters (256 bits) for HS256
    private final String secret = "brutal_direct_authoritative_secret_key_12345";
    private final long ttl = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, ttl);
    }

    @Test
    void generateToken_ShouldProduceValidString() {
        String userId = "123";
        String token = jwtService.generateToken(userId);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // JWT format: header.payload.signature
    }

    @Test
    void extractUserId_ShouldReturnCorrectId() {
        String userId = "999";
        String token = jwtService.generateToken(userId);

        String extractedId = jwtService.extractUserId(token);
        assertEquals(userId, extractedId);
    }

    @Test
    void isTokenValid_WithValidToken_ShouldReturnTrue() {
        String token = jwtService.generateToken("1");
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_WithMalformedToken_ShouldReturnFalse() {
        assertFalse(jwtService.isTokenValid("not.a.real.token"));
    }

    @Test
    void isTokenValid_WithExpiredToken_ShouldReturnFalse() {
        // Create a service with 0 TTL to force immediate expiry
        JwtService expiredService = new JwtService(secret, -1000);
        String token = expiredService.generateToken("1");

        assertFalse(expiredService.isTokenValid(token));
    }
}