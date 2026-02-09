package com.example.transitcard.security;

import com.example.transitcard.repository.TransitCardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransitCardRepository transitCardRepository;

    /**
     * This mock is CRITICAL. Your logs show JwtService fails to create
     * because of missing environment variables. Mocking it bypasses
     * that initialization entirely.
     */
    @MockitoBean
    private JwtService jwtService;

    @Test
    void unauthenticatedRequest_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/transit/my-cards"))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsConfiguration_ShouldAllowSpecificOrigin() throws Exception {
        mockMvc.perform(options("/api/transit/buy")
                        .header("Origin", "http://localhost:3002")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3002"));
    }
}