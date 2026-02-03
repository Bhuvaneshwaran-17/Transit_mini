package com.example.transitcard.service;

import com.example.transitcard.model.CardType;
import com.example.transitcard.model.TransitCard;
import com.example.transitcard.repository.TransitCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TransitCardService {

    private final TransitCardRepository repository;

    public TransitCardService(TransitCardRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Optional<TransitCard> buyCard(TransitCard card, Long userId) {

        boolean alreadyHasThisType =
                repository.existsByUserIdAndCardType(userId, card.getCardType());

        if (alreadyHasThisType) {
            return Optional.empty();
        }

        card.setUserId(userId);
        card.setCardNumber(
                "TC-" + card.getCardType() + "-" + System.currentTimeMillis() + "-" + userId
        );
        card.setCardActive(true);

        return Optional.of(repository.save(card));
    }

    public List<TransitCard> getMyCards(Long userId) {
        return repository.findByUserId(userId);
    }
}
