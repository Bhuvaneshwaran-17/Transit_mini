package com.example.transitcard.repository;

import com.example.transitcard.model.CardType;
import com.example.transitcard.model.TransitCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransitCardRepository extends JpaRepository<TransitCard, Long> {

    boolean existsByUserIdAndCardType(Long userId, CardType cardType);
    Optional<TransitCard> findByUserIdAndCardType(Long userId, CardType cardType);
    List<TransitCard> findByUserId(Long userId);
}
