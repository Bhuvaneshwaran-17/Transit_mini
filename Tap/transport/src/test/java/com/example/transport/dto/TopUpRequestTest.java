package com.example.transport.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TopUpRequestTest - Validating data flow and constraints.
 */
class TopUpRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testGettersAndSetters() {
        // Arrange
        TopUpRequest request = new TopUpRequest();
        BigDecimal amount = new BigDecimal("100.00");
        String cardType = "REGULAR";

        // Act
        request.setAmount(amount);
        request.setCardType(cardType);

        // Assert
        assertEquals(amount, request.getAmount());
        assertEquals(cardType, request.getCardType());
    }

    @Test
    void testValidation_Success() {
        TopUpRequest request = new TopUpRequest();
        request.setAmount(new BigDecimal("20.00"));
        request.setCardType("STUDENT");

        Set<ConstraintViolation<TopUpRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should not have violations");
    }

    @Test
    void testValidation_Failure_NullAndBlank() {
        TopUpRequest request = new TopUpRequest(); // Null amount, null cardType

        Set<ConstraintViolation<TopUpRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("amount is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cardType is required")));
    }

    @Test
    void testValidation_Failure_InvalidAmount() {
        TopUpRequest request = new TopUpRequest();
        request.setAmount(new BigDecimal("0.00")); // Fails @DecimalMin
        request.setCardType("REGULAR");

        Set<ConstraintViolation<TopUpRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("amount must be greater than 0")));
    }
}