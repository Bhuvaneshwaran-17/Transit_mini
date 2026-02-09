package com.example.transitcard.service;

import com.example.transitcard.model.CardType;
import com.example.transitcard.model.TransitCard;
import com.example.transitcard.repository.TransitCardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransitCardServiceTest {

    @Mock
    private TransitCardRepository repository;

    @InjectMocks
    private TransitCardService service;

    @Test
    void buyCard_ShouldReturnEmpty_WhenUserAlreadyHasCardType() {
        // Arrange
        Long userId = 1L;
        TransitCard cardRequest = new TransitCard();
        cardRequest.setCardType(CardType.STUDENT);

        when(repository.existsByUserIdAndCardType(userId, CardType.STUDENT)).thenReturn(true);

        // Act
        Optional<TransitCard> result = service.buyCard(cardRequest, userId);

        // Assert
        assertTrue(result.isEmpty(), "Should return empty when the user already owns this card type");
        verify(repository, never()).save(any());
    }

    @Test
    void buyCard_ShouldSaveAndReturnCard_WhenUserDoesNotHaveCardType() {
        // Arrange
        Long userId = 1L;
        TransitCard cardRequest = new TransitCard();
        cardRequest.setCardType(CardType.REGULAR);

        when(repository.existsByUserIdAndCardType(userId, CardType.REGULAR)).thenReturn(false);
        // Return the saved card so we can inspect the generated fields
        when(repository.save(any(TransitCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<TransitCard> result = service.buyCard(cardRequest, userId);

        // Assert
        assertTrue(result.isPresent());
        TransitCard savedCard = result.get();
        assertEquals(userId, savedCard.getUserId());
        assertEquals(Boolean.TRUE, savedCard.getCardActive());
        assertNotNull(savedCard.getCardNumber());
        assertTrue(savedCard.getCardNumber().startsWith("TC-REGULAR-"));
        verify(repository, times(1)).save(any());
    }

    @Test
    void getMyCards_ShouldReturnListOfCards() {
        // Arrange
        Long userId = 1L;
        List<TransitCard> mockCards = List.of(new TransitCard(), new TransitCard());
        when(repository.findByUserId(userId)).thenReturn(mockCards);

        // Act
        List<TransitCard> result = service.getMyCards(userId);

        // Assert
        assertEquals(2, result.size());
        verify(repository, times(1)).findByUserId(userId);
    }
}