package com.example.transport.service;

import com.example.transport.entity.Card;
import com.example.transport.repository.CardRepository;
import com.example.transport.service.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CardServiceTest {

    private CardRepository cardRepository;
    private CardService cardService;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        cardService = new CardService(cardRepository);
    }

    private Card createCard(Long userId, BigDecimal balance, String type) {
        Card card = new Card();
        card.setUserId(userId);
        card.setBalance(balance);
        card.setCardType(type);
        card.setCardActive(true);
        return card;
    }

    @Test
    void testDeductFareSuccess() {
        Card card = createCard(1L, BigDecimal.valueOf(100), "REGULAR");
        when(cardRepository.findByUserId(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card updated = cardService.deductFare(1L, BigDecimal.valueOf(30));

        assertEquals(BigDecimal.valueOf(70), updated.getBalance());
        verify(cardRepository).save(card);
    }

    @Test
    void testDeductFareCardNotFound() {
        when(cardRepository.findByUserId(1L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> cardService.deductFare(1L, BigDecimal.TEN));

        assertEquals("Card not found", ex.getMessage());
    }

    @Test
    void testDeductFareInsufficientBalance() {
        Card card = createCard(1L, BigDecimal.valueOf(5), "REGULAR");
        when(cardRepository.findByUserId(1L)).thenReturn(Optional.of(card));

        CardService.InsufficientBalanceException ex =
                assertThrows(CardService.InsufficientBalanceException.class,
                        () -> cardService.deductFare(1L, BigDecimal.TEN));

        assertEquals("Insufficient balance", ex.getMessage());
        assertEquals(BigDecimal.valueOf(5), ex.getBalance());
    }

    @Test
    void testTopUpSuccess() {
        Card card = createCard(1L, BigDecimal.valueOf(50), "REGULAR");
        when(cardRepository.findByUserId(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card updated = cardService.topUp(1L, BigDecimal.valueOf(25));

        assertEquals(BigDecimal.valueOf(75), updated.getBalance());
        verify(cardRepository).save(card);
    }

    @Test
    void testTopUpCardNotFound() {
        when(cardRepository.findByUserId(1L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> cardService.topUp(1L, BigDecimal.valueOf(25)));

        assertEquals("Card not found", ex.getMessage());
    }

    @Test
    void testTopUpWithRulesCardNotFound() {
        when(cardRepository.findByUserId(1L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> cardService.topUpWithRules(1L));

        assertEquals("Card not found", ex.getMessage());
    }

    @Test
    void testTopUpWithRulesIncrementApplied() {
        Card card = createCard(1L, BigDecimal.valueOf(100), "REGULAR");
        // manually set increment since your service uses a field
        cardService.increment = BigDecimal.valueOf(60);

        when(cardRepository.findByUserId(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card updated = cardService.topUpWithRules(1L);

        assertEquals(BigDecimal.valueOf(160), updated.getBalance());
        verify(cardRepository).save(card);
    }
}
