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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
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

@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest(TransitCardController.class)// disables security filters
public class TransitCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransitCardService service;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Helper Principal
    private Principal mockPrincipal() {
        return () -> "1"; // userId = 1
    }

    // Test 1: Buy card successfully
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

    // Test 2: Buy card when user already has one (409)
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

    // Test 3: Get cards successfully
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

    // Test 4: Get cards when user has none (404)
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
