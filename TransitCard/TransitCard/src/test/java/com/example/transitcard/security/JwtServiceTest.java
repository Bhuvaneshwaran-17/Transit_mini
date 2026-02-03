package com.example.transitcard.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private SecretKey key;

    private static final String SECRET =
            "my-super-secret-key-my-super-secret-key-12345";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
        key = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    @Test
    void extractUserId_shouldReturnSubject() {
        String token = generateToken("1", 60_000);
        assertEquals("1", jwtService.extractUserId(token));
    }

    @Test
    void isTokenValid_shouldReturnTrue_forValidToken() {
        String token = generateToken("1", 60_000);
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_shouldReturnFalse_forExpiredToken() {
        String token = generateToken("1", -60_000);
        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_shouldReturnFalse_forMalformedToken() {
        assertFalse(jwtService.isTokenValid("bad.token.value"));
    }

    private String generateToken(String userId, long expiryMillis) {
        return Jwts.builder()
                .setSubject(userId)
                .setExpiration(new Date(System.currentTimeMillis() + expiryMillis))
                .signWith(key)
                .compact();
    }
}
