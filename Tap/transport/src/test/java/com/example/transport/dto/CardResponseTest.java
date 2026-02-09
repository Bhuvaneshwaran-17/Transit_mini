package com.example.transport.dto;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Bhuvi, this covers the constructor and all Lombok-generated getters.
 */
class CardResponseTest {

    @Test
    void testCardResponse_GetterAndConstructor() {
        // Arrange
        boolean expectedSuccess = true;
        String expectedMessage = "Success Message";
        BigDecimal expectedBalance = new BigDecimal("100.50");

        // Act
        CardResponse response = new CardResponse(expectedSuccess, expectedMessage, expectedBalance);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(expectedMessage, response.getMessage());
        assertEquals(expectedBalance, response.getBalance());
    }
}