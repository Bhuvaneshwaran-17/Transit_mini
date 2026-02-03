package com.example.transport.service;

import com.example.transport.entity.CardType;
import com.example.transport.entity.TransitCard;
import com.example.transport.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    /**
     * Deduct a fare amount from a SPECIFIC card type
     */
    @Transactional
    public TransitCard deductFare(Long userId, String cardType, BigDecimal fare) {
        // TRUTH: We search by BOTH userId and cardType now
        TransitCard card = cardRepository.findByUserIdAndCardType(userId, CardType.valueOf(cardType))
                .orElseThrow(() -> new IllegalArgumentException("Card of type " + cardType + " not found for user"));

        if (!Boolean.TRUE.equals(card.getCardActive())) {
            throw new IllegalStateException("This " + cardType + " card is not active");
        }

        if (card.getBalance().compareTo(fare) < 0) {
            throw new InsufficientBalanceException("Insufficient balance on " + cardType + " card", card.getBalance());
        }

        card.setBalance(card.getBalance().subtract(fare));
        return cardRepository.save(card);
    }

    /**
     * Top up a SPECIFIC card type with a custom amount
     */
    @Transactional
    public TransitCard topUp(Long userId, String cardType, BigDecimal amount) {
        TransitCard card = cardRepository.findByUserIdAndCardType(userId, CardType.valueOf(cardType))
                .orElseThrow(() -> new IllegalArgumentException("Card of type " + cardType + " not found"));

        if (!Boolean.TRUE.equals(card.getCardActive())) {
            throw new IllegalStateException("Card is not active");
        }

        card.setBalance(card.getBalance().add(amount));
        return cardRepository.save(card);
    }

    /**
     * Logic for automatic increments based on type
     */
    @Transactional
    public TransitCard topUpWithRules(Long userId, String cardType) {
        TransitCard card = cardRepository.findByUserIdAndCardType(userId, CardType.valueOf(cardType))
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        if (!Boolean.TRUE.equals(card.getCardActive())) {
            throw new IllegalStateException("Card is not active");
        }

        BigDecimal increment;
        if ("STUDENT".equalsIgnoreCase(String.valueOf(card.getCardType()))) {
            increment = BigDecimal.valueOf(40);
        } else if ("REGULAR".equalsIgnoreCase(String.valueOf(card.getCardType()))) {
            increment = BigDecimal.valueOf(60);
        } else {
            throw new IllegalArgumentException("Unknown card type: " + card.getCardType());
        }

        card.setBalance(card.getBalance().add(increment));
        return cardRepository.save(card);
    }

    public static class InsufficientBalanceException extends RuntimeException {
        private final BigDecimal balance;
        public InsufficientBalanceException(String message, BigDecimal balance) {
            super(message);
            this.balance = balance;
        }
        public BigDecimal getBalance() { return balance; }
    }
}