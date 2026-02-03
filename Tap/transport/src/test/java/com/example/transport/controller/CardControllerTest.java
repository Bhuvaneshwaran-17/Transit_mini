package com.example.transport.controller;

import com.example.transport.controller.CardController;
import com.example.transport.dto.CardResponse;
import com.example.transport.dto.TapRequest;
import com.example.transport.dto.TopUpRequest;
import com.example.transport.entity.Card;
import com.example.transport.service.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CardControllerTest {

    private CardService cardService;
    private CardController cardController;
    private Principal principal;

    @BeforeEach
    void setUp() {
        cardService = mock(CardService.class);
        cardController = new CardController(cardService);
        principal = () -> "1"; // simulate JWT userId = 1
    }

    @Test
    void testTapSuccess() {
        TapRequest request = new TapRequest();
        request.setFare(BigDecimal.valueOf(20));

        Card card = new Card();
        card.setBalance(BigDecimal.valueOf(80));

        when(cardService.deductFare(1L, BigDecimal.valueOf(20))).thenReturn(card);

        ResponseEntity<CardResponse> response = cardController.tap(request, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Fare deducted successfully", response.getBody().getMessage());
        assertEquals(BigDecimal.valueOf(80), response.getBody().getBalance());
    }

    @Test
    void testTapFailure() {
        TapRequest request = new TapRequest();
        request.setFare(BigDecimal.valueOf(200));

        when(cardService.deductFare(1L, BigDecimal.valueOf(200)))
                .thenThrow(new RuntimeException("Insufficient balance"));

        ResponseEntity<CardResponse> response = cardController.tap(request, principal);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Insufficient balance", response.getBody().getMessage());
        assertNull(response.getBody().getBalance());
    }

    @Test
    void testTopUpSuccess() {
        TopUpRequest request = new TopUpRequest();
        request.setAmount(BigDecimal.valueOf(50));

        Card card = new Card();
        card.setBalance(BigDecimal.valueOf(150));

        when(cardService.topUp(1L, BigDecimal.valueOf(50))).thenReturn(card);

        ResponseEntity<CardResponse> response = cardController.topUp(request, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Top-up successful", response.getBody().getMessage());
        assertEquals(BigDecimal.valueOf(150), response.getBody().getBalance());
    }

    @Test
    void testTopUpFailure() {
        TopUpRequest request = new TopUpRequest();
        request.setAmount(BigDecimal.valueOf(-10));

        when(cardService.topUp(1L, BigDecimal.valueOf(-10)))
                .thenThrow(new RuntimeException("Invalid amount"));

        ResponseEntity<CardResponse> response = cardController.topUp(request, principal);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid amount", response.getBody().getMessage());
        assertNull(response.getBody().getBalance());
    }
}
