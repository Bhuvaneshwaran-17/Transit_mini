package com.example.transport.repository;

import com.example.transport.entity.TransitCard;
import com.example.transport.entity.CardType;
import com.example.transport.entity.TransitCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepository extends JpaRepository<TransitCard, Long> {
    Optional<TransitCard> findByUserIdAndCardType(Long userId, CardType cardType);
}
