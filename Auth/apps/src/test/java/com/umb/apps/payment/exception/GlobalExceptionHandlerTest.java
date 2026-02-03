package com.umb.apps.payment.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgument_ShouldReturnConflict() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Invalid data");

        // Act
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Invalid data", response.getBody().get("error"));
    }

    @Test
    void handleConstraint_ShouldReturnConflict() {
        // Arrange
        DataIntegrityViolationException ex = new DataIntegrityViolationException("DB Error");

        // Act
        ResponseEntity<Map<String, String>> response = handler.handleConstraint(ex);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("User already exists", response.getBody().get("error"));
    }

    @Test
    void handleValidation_ShouldReturnBadRequestWithFieldErrors() {
        // Arrange: Mocking the complex structure of MethodArgumentNotValidException
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "username", "Required field");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // Act
        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().containsKey("username"));
        assertEquals("Required field", response.getBody().get("username"));
    }
}