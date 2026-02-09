package com.umb.apps.payment.config;

import com.umb.apps.payment.security.JwtAuthenticationFilter;
import com.umb.apps.payment.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SecurityConfigTest - Focused exclusively on Security Rules.
 * Bhuvi, we are using the public doFilter to bypass the 'protected' access error.
 */
@WebMvcTest
@ContextConfiguration(classes = {SecurityConfig.class})
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setup() throws ServletException, IOException {
        // We target doFilter (public) instead of doFilterInternal (protected)
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void publicEndpoints_ShouldBeAccessible() throws Exception {
        // This should return 404 (Not Found) because the path is permitted
        // but no controller exists in this test context.
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedEndpoints_ShouldReturnForbidden_WhenNoToken() throws Exception {
        // This should return 403 (Forbidden) because security should block it.
        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isForbidden());
    }
}