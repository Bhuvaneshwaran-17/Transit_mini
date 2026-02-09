package com.example.transitcard.controller;

import com.example.transitcard.model.CardType;
import com.example.transitcard.model.TransitCard;
import com.example.transitcard.security.JwtAuthenticationFilter;
import com.example.transitcard.security.JwtService;
import com.example.transitcard.service.TransitCardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Corrected Test class for TransitCardController.
 * Bhuvi, notice the fixed imports for WebMvcTest and AutoConfigureMockMvc.
 */
@WebMvcTest(TransitCardController.class)
@AutoConfigureMockMvc(addFilters = false) // Disables security for this specific test
public class TransitCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransitCardService service;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService; // Added to satisfy security context

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Helper Principal
    private Principal mockPrincipal() {
        return () -> "1"; // userId = 1
    }

    @Test
    void buyCard_shouldReturn200_whenCardPurchased() throws Exception {
        TransitCard card = new TransitCard();
        card.setCardType(CardType.STUDENT);
        card.setBalance(40.0);

        Mockito.when(service.buyCard(any(TransitCard.class), eq(1L)))
                .thenReturn(Optional.of(card));

        mockMvc.perform(post("/api/transit/buy")
                        .principal(mockPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(card)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardType").value("STUDENT"))
                .andExpect(jsonPath("$.balance").value(40.0));
    }

    @Test
    void buyCard_shouldReturn409_whenUserAlreadyHasCard() throws Exception {
        TransitCard card = new TransitCard();

        Mockito.when(service.buyCard(any(TransitCard.class), eq(1L)))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/transit/buy")
                        .principal(mockPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(card)))
                .andExpect(status().isConflict())
                .andExpect(content().string("You already own this type of card."));
    }

    @Test
    void getMyCards_shouldReturn200_whenCardsExist() throws Exception {
        TransitCard card = new TransitCard();
        card.setCardType(CardType.STUDENT);

        Mockito.when(service.getMyCards(1L))
                .thenReturn(List.of(card));

        mockMvc.perform(get("/api/transit/my-cards")
                        .principal(mockPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardType").value("STUDENT"));
    }

    @Test
    void getMyCards_shouldReturn404_whenNoCardsExist() throws Exception {
        Mockito.when(service.getMyCards(1L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/transit/my-cards")
                        .principal(mockPrincipal()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("You need to buy a card first."));
    }
}