package com.example.transitcard.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
        name = "transit_card",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "card_type"})
        }
)
@Data
public class TransitCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType;

    @Column(nullable = false)
    private Double balance = 0.0;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Boolean cardActive = true;
}
