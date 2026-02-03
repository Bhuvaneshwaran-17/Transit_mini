package com.example.transport.security;

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
        jwtService = new JwtService(secret);
    }



    @Test
    void isTokenValid_WithMalformedToken_ShouldReturnFalse() {
        assertFalse(jwtService.isTokenValid("not.a.real.token"));
    }

}
