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

class TapRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testGettersAndSetters() {
        // Arrange
        TapRequest request = new TapRequest();
        BigDecimal fare = new BigDecimal("5.50");
        String cardType = "STUDENT";

        // Act
        request.setFare(fare);
        request.setCardType(cardType);

        // Assert
        assertEquals(fare, request.getFare());
        assertEquals(cardType, request.getCardType());
    }

    @Test
    void testValidation_Success() {
        TapRequest request = new TapRequest();
        request.setFare(new BigDecimal("1.00"));
        request.setCardType("REGULAR");

        Set<ConstraintViolation<TapRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    void testValidation_Failure_EmptyFields() {
        TapRequest request = new TapRequest(); // Fields are null/blank

        Set<ConstraintViolation<TapRequest>> violations = validator.validate(request);

        // Assert: fare is null, cardType is blank
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("fare is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cardType is required")));
    }

    @Test
    void testValidation_Failure_LowFare() {
        TapRequest request = new TapRequest();
        request.setFare(new BigDecimal("0.00")); // Below DecimalMin
        request.setCardType("STUDENT");

        Set<ConstraintViolation<TapRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("fare must be greater than 0")));
    }
}