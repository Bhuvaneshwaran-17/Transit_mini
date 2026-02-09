package com.example.transitcard.exception;

import com.example.transitcard.controller.TransitCardController;
import com.example.transitcard.service.TransitCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @Mock
    private TransitCardService transitCardService;

    @InjectMocks
    private TransitCardController transitCardController;

    @BeforeEach
    void setUp() {
        // We manually build the MockMvc context to include your Exception Handler
        // This bypasses the "Handler: Type = null" issue entirely
        mockMvc = MockMvcBuilders.standaloneSetup(transitCardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleRuntimeException_ShouldReturnBadRequest_WhenBuyCardFails() throws Exception {
        // Arrange
        String expectedMessage = "Bhuvi, the simulation failed!";
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("1");

        when(transitCardService.buyCard(any(), anyLong()))
                .thenThrow(new RuntimeException(expectedMessage));

        // Act & Assert
        // In standalone mode, we don't need the context path if one exists
        mockMvc.perform(post("/api/transit/buy")
                        .principal(mockPrincipal) // Manually inject the principal
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardType\":\"STUDENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(expectedMessage));
    }
}