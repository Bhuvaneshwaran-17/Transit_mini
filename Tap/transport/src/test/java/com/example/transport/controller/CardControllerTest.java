package com.example.transport.controller;

import com.example.transport.dto.TapRequest;
import com.example.transport.dto.TopUpRequest;
import com.example.transport.entity.TransitCard;
import com.example.transport.security.JwtAuthenticationFilter;
import com.example.transport.security.JwtService;
import com.example.transport.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CardControllerTest - Final Version for 100% Coverage
 * Bhuvi, this version correctly bypasses the protected access modifier issues.
 */
@WebMvcTest(CardController.class)
public class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private TransitCard mockCard;

    @BeforeEach
    void setUp() throws Exception {
        mockCard = new TransitCard();
        mockCard.setBalance(new BigDecimal("100.00"));

        // Use the PUBLIC doFilter method to avoid the 'protected access' error
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "1")
    void tap_Success() throws Exception {
        when(cardService.deductFare(anyLong(), anyString(), any())).thenReturn(mockCard);

        TapRequest request = new TapRequest();
        request.setCardType("METRO");
        request.setFare(new BigDecimal("2.50"));

        mockMvc.perform(post("/api/card/tap")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Fare deducted"))
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    @WithMockUser(username = "1")
    void topUp_Success() throws Exception {
        when(cardService.topUp(anyLong(), anyString(), any())).thenReturn(mockCard);

        TopUpRequest request = new TopUpRequest();
        request.setCardType("BUS");
        request.setAmount(new BigDecimal("50.00"));

        mockMvc.perform(post("/api/card/topup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Top-up successful"))
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    @WithMockUser(username = "1")
    void topUp_Failure_CatchBlock() throws Exception {
        // Specifically throw an exception to test the catch block in the controller
        when(cardService.topUp(anyLong(), anyString(), any()))
                .thenThrow(new RuntimeException("Card Service Failure"));

        TopUpRequest request = new TopUpRequest();
        request.setCardType("BUS");
        request.setAmount(new BigDecimal("50.00"));

        mockMvc.perform(post("/api/card/topup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Card Service Failure"));
    }
}