package com.example.transport.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class CardResponse {
    private final boolean success;
    private final String message;
    private final BigDecimal balance;

    public CardResponse(boolean success, String message, BigDecimal balance) {
        this.success = success;
        this.message = message;
        this.balance = balance;
    }

}
