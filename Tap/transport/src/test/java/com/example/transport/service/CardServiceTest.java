package com.example.transport.service;

import com.example.transport.entity.CardType;
import com.example.transport.entity.TransitCard;
import com.example.transport.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    private TransitCard makeCard(CardType type, boolean active, String balance) {
        TransitCard card = new TransitCard();
        card.setCardType(type);
        card.setCardActive(active);
        card.setBalance(new BigDecimal(balance));
        return card;
    }

    // -------------------------
    // deductFare()
    // -------------------------

    @Test
    void deductFare_success_deductsBalanceAndSaves() {
        Long userId = 10L;
        String cardType = "STUDENT";
        BigDecimal fare = new BigDecimal("25.50");

        TransitCard card = makeCard(CardType.STUDENT, true, "100.00");

        when(cardRepository.findByUserIdAndCardType(userId, CardType.STUDENT))
                .thenReturn(Optional.of(card));
        when(cardRepository.save(any(TransitCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransitCard result = cardService.deductFare(userId, cardType, fare);

        assertEquals(new BigDecimal("74.50"), result.getBalance());

        ArgumentCaptor<TransitCard> captor = ArgumentCaptor.forClass(TransitCard.class);
        verify(cardRepository).save(captor.capture());
        assertEquals(new BigDecimal("74.50"), captor.getValue().getBalance());
    }

    @Test
    void deductFare_cardNotFound_throwsIllegalArgumentException() {
        Long userId = 10L;
        String cardType = "STUDENT";

        when(cardRepository.findByUserIdAndCardType(userId, CardType.STUDENT))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cardService.deductFare(userId, cardType, new BigDecimal("10.00"))
        );

        assertTrue(ex.getMessage().contains("Card of type STUDENT not found"));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void deductFare_inactiveCard_throwsIllegalStateException() {
        Long userId = 10L;
        String cardType = "STUDENT";

        TransitCard inactive = makeCard(CardType.STUDENT, false, "100.00");

        when(cardRepository.findByUserIdAndCardType(userId, CardType.STUDENT))
                .thenReturn(Optional.of(inactive));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> cardService.deductFare(userId, cardType, new BigDecimal("10.00"))
        );

        assertTrue(ex.getMessage().toLowerCase().contains("not active"));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void deductFare_insufficientBalance_throwsInsufficientBalanceException() {
        Long userId = 10L;
        String cardType = "STUDENT";

        TransitCard card = makeCard(CardType.STUDENT, true, "20.00");

        when(cardRepository.findByUserIdAndCardType(userId, CardType.STUDENT))
                .thenReturn(Optional.of(card));

        CardService.InsufficientBalanceException ex = assertThrows(
                CardService.InsufficientBalanceException.class,
                () -> cardService.deductFare(userId, cardType, new BigDecimal("50.00"))
        );

        assertEquals(new BigDecimal("20.00"), ex.getBalance());
        verify(cardRepository, never()).save(any());
    }

    @Test
    void deductFare_invalidCardTypeString_throwsIllegalArgumentException() {
        // CardType.valueOf("VIP") will throw before repository is called
        assertThrows(IllegalArgumentException.class,
                () -> cardService.deductFare(10L, "VIP", new BigDecimal("10.00")));

        verifyNoInteractions(cardRepository);
    }

    // -------------------------
    // topUp()
    // -------------------------

    @Test
    void topUp_success_addsAmountAndSaves() {
        Long userId = 20L;
        String cardType = "REGULAR";
        BigDecimal amount = new BigDecimal("60.00");

        TransitCard card = makeCard(CardType.REGULAR, true, "40.00");

        when(cardRepository.findByUserIdAndCardType(userId, CardType.REGULAR))
                .thenReturn(Optional.of(card));
        when(cardRepository.save(any(TransitCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransitCard result = cardService.topUp(userId, cardType, amount);

        assertEquals(new BigDecimal("100.00"), result.getBalance());
        verify(cardRepository).save(any(TransitCard.class));
    }

    @Test
    void topUp_cardNotFound_throwsIllegalArgumentException() {
        Long userId = 20L;
        String cardType = "REGULAR";

        when(cardRepository.findByUserIdAndCardType(userId, CardType.REGULAR))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> cardService.topUp(userId, cardType, new BigDecimal("10.00")));

        verify(cardRepository, never()).save(any());
    }

    @Test
    void topUp_inactiveCard_throwsIllegalStateException() {
        Long userId = 20L;
        String cardType = "REGULAR";

        TransitCard inactive = makeCard(CardType.REGULAR, false, "40.00");

        when(cardRepository.findByUserIdAndCardType(userId, CardType.REGULAR))
                .thenReturn(Optional.of(inactive));

        assertThrows(IllegalStateException.class,
                () -> cardService.topUp(userId, cardType, new BigDecimal("10.00")));

        verify(cardRepository, never()).save(any());
    }

    @Test
    void topUp_invalidCardTypeString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> cardService.topUp(20L, "VIP", new BigDecimal("10.00")));

        verifyNoInteractions(cardRepository);
    }

    // -------------------------
    // topUpWithRules()
    // -------------------------

    @Test
    void topUpWithRules_student_adds40() {
        Long userId = 30L;
        TransitCard student = makeCard(CardType.STUDENT, true, "10.00");

        when(cardRepository.findByUserIdAndCardType(userId, CardType.STUDENT))
                .thenReturn(Optional.of(student));
        when(cardRepository.save(any(TransitCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransitCard result = cardService.topUpWithRules(userId, "STUDENT");

        assertEquals(new BigDecimal("50.00"), result.getBalance());
        verify(cardRepository).save(any(TransitCard.class));
    }

    @Test
    void topUpWithRules_regular_adds60() {
        Long userId = 30L;
        TransitCard regular = makeCard(CardType.REGULAR, true, "10.00");

        when(cardRepository.findByUserIdAndCardType(userId, CardType.REGULAR))
                .thenReturn(Optional.of(regular));
        when(cardRepository.save(any(TransitCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransitCard result = cardService.topUpWithRules(userId, "REGULAR");

        assertEquals(new BigDecimal("70.00"), result.getBalance());
        verify(cardRepository).save(any(TransitCard.class));
    }

    @Test
    void topUpWithRules_cardNotFound_throwsIllegalArgumentException() {
        Long userId = 30L;

        when(cardRepository.findByUserIdAndCardType(userId, CardType.STUDENT))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> cardService.topUpWithRules(userId, "STUDENT"));

        verify(cardRepository, never()).save(any());
    }

    @Test
    void topUpWithRules_inactiveCard_throwsIllegalStateException() {
        Long userId = 30L;
        TransitCard inactive = makeCard(CardType.STUDENT, false, "10.00");

        when(cardRepository.findByUserIdAndCardType(userId, CardType.STUDENT))
                .thenReturn(Optional.of(inactive));

        assertThrows(IllegalStateException.class,
                () -> cardService.topUpWithRules(userId, "STUDENT"));

        verify(cardRepository, never()).save(any());
    }

    @Test
    void topUpWithRules_invalidCardTypeString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> cardService.topUpWithRules(30L, "VIP"));

        verifyNoInteractions(cardRepository);
    }
}