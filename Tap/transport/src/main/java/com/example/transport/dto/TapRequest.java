package com.example.transport.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Setter
@Getter
public class TapRequest {
    @NotNull(message = "fare is required")
    @DecimalMin(value = "0.01", message = "fare must be greater than 0")
    private BigDecimal fare;

    @NotBlank(message = "cardType is required")
    private String cardType; // "STUDENT" or "REGULAR"
}