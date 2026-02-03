package com.example.transport.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity
@Table(name = "transit_card")
@Data
public class TransitCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", nullable = false, unique = true, length = 16)
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 16)
    private CardType cardType; // Ensure CardType enum is identical in both services

    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal balance = java.math.BigDecimal.ZERO;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "card_active", nullable = false)
    private Boolean cardActive = true;
}