//package com.example.transitcard.service;
//
//import com.example.transitcard.model.CardType;
//import com.example.transitcard.model.TransitCard;
//import com.example.transitcard.repository.TransitCardRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class TransitCardServiceTest {
//
//    @Mock
//    private TransitCardRepository repository;
//
//    @InjectMocks
//    private TransitCardService service;
//
//    // ✅ Test 1: Buy STUDENT card successfully
//    @Test
//    void shouldBuyStudentCardSuccessfully() {
//
//        TransitCard card = new TransitCard();
//        card.setCardType(CardType.STUDENT);
//
//        Long userId = 1L;
//
//        when(repository.existsByUserIdAndCardType(userId, CardType.STUDENT))
//                .thenReturn(false);
//
//        when(repository.save(any(TransitCard.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        Optional<TransitCard> result = service.buyCard(card, userId);
//
//        assertTrue(result.isPresent());
//        assertEquals(CardType.STUDENT, result.get().getCardType());
//        assertEquals(userId, result.get().getUserId());
//        assertTrue(result.get().isCardActive());
//
//        verify(repository).save(card);
//    }
//
//    // ❌ Test 2: Prevent duplicate STUDENT card
//    @Test
//    void shouldNotAllowDuplicateStudentCard() {
//
//        TransitCard card = new TransitCard();
//        card.setCardType(CardType.STUDENT);
//
//        Long userId = 1L;
//
//        when(repository.existsByUserIdAndCardType(userId, CardType.STUDENT))
//                .thenReturn(true);
//
//        Optional<TransitCard> result = service.buyCard(card, userId);
//
//        assertTrue(result.isEmpty());
//
//        verify(repository, never()).save(any());
//    }
//
//    // ✅ Test 3: Buy REGULAR card successfully
//    @Test
//    void shouldBuyRegularCardSuccessfully() {
//
//        TransitCard card = new TransitCard();
//        card.setCardType(CardType.REGULAR);
//
//        Long userId = 1L;
//
//        when(repository.existsByUserIdAndCardType(userId, CardType.REGULAR))
//                .thenReturn(false);
//
//        when(repository.save(any(TransitCard.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        Optional<TransitCard> result = service.buyCard(card, userId);
//
//        assertTrue(result.isPresent());
//        assertEquals(CardType.REGULAR, result.get().getCardType());
//
//        verify(repository).save(card);
//    }
//
//    // ❌ Test 4: Prevent duplicate REGULAR card
//    @Test
//    void shouldNotAllowDuplicateRegularCard() {
//
//        TransitCard card = new TransitCard();
//        card.setCardType(CardType.REGULAR);
//
//        Long userId = 1L;
//
//        when(repository.existsByUserIdAndCardType(userId, CardType.REGULAR))
//                .thenReturn(true);
//
//        Optional<TransitCard> result = service.buyCard(card, userId);
//
//        assertTrue(result.isEmpty());
//
//        verify(repository, never()).save(any());
//    }
//
//    // ✅ Test 5: Get user cards
//    @Test
//    void shouldReturnUserCards() {
//
//        Long userId = 1L;
//
//        TransitCard studentCard = new TransitCard();
//        studentCard.setCardType(CardType.STUDENT);
//
//        TransitCard regularCard = new TransitCard();
//        regularCard.setCardType(CardType.REGULAR);
//
//        when(repository.findByUserId(userId))
//                .thenReturn(List.of(studentCard, regularCard));
//
//        List<TransitCard> cards = service.getMyCards(userId);
//
//        assertEquals(2, cards.size());
//        verify(repository).findByUserId(userId);
//    }
//}
