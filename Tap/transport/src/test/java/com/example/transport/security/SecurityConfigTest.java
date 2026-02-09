package com.example.transport.security;

import com.example.transport.controller.CardController;
import com.example.transport.service.CardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@Import(SecurityConfig.class)
public class SecurityConfigTest {

    @BeforeEach
    void setup() throws Exception {
        // This tells the mock filter to let the request continue to the NEXT filter
        // where Spring's real security checks happen.
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    // We MUST mock these because they are dependencies of SecurityConfig or the Filter
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private CardService cardService;

    @Test
    void testBeansExist() {
        assertNotNull(corsConfigurationSource);
    }

    @Test
    void protectedEndpoint_ShouldReturnForbidden_WhenNoUser() throws Exception {
        // We expect 403 because no @WithMockUser is provided
        mockMvc.perform(get("/api/card/tap"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testUser")
    void protectedEndpoint_ShouldNotReturnForbidden_WhenUserPresent() throws Exception {
        // When a user IS present, it should bypass 403.
        // It might return 405 (Method Not Allowed) because /tap is a POST, not a GET.
        mockMvc.perform(get("/api/card/tap"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testCorsConfiguration() throws Exception {
        mockMvc.perform(options("/api/card/tap")
                        .header("Origin", "http://localhost:3002")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3002"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}