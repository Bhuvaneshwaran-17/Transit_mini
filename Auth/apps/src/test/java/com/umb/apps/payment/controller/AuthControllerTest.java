package com.umb.apps.payment.controller;

import com.umb.apps.payment.controller.AuthController;
import com.umb.apps.payment.dto.AuthResponseDto;
import com.umb.apps.payment.dto.LoginRequestDto;
import com.umb.apps.payment.entity.User;
import com.umb.apps.payment.repository.UserRepository;
import com.umb.apps.payment.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthController authController;

    private LoginRequestDto loginRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequestDto("9876543210", "password123");

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setPhoneNumber("9876543210");
    }

    @Test
    void login_Success_ShouldReturnTokenWithUserId() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByPhoneNumber(loginRequest.getPhoneNumber())).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken("1")).thenReturn("mocked-jwt-token");

        // Act
        ResponseEntity<AuthResponseDto> response = authController.login(loginRequest);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("mocked-jwt-token", response.getBody().getToken());

        // Verify we used the ID to generate the token, NOT the phone number
        verify(jwtService).generateToken("1");
        verify(jwtService, never()).generateToken("9876543210");
    }

    @Test
    void login_InvalidCredentials_ShouldReturn401() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act
        ResponseEntity<AuthResponseDto> response = authController.login(loginRequest);

        // Assert
        assertEquals(401, response.getStatusCode().value());
        assertNull(response.getBody().getToken());
    }

    @Test
    void login_UserNotFoundAfterAuth_ShouldReturn401() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByPhoneNumber(loginRequest.getPhoneNumber())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<AuthResponseDto> response = authController.login(loginRequest);

        // Assert
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void login_UnexpectedException_ShouldReturn500() {
        // Arrange: Force the authenticationManager to throw a generic RuntimeException
        // This bypasses the BadCredentialsException catch and hits the generic Exception catch
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Database is down or connection failed"));

        // Act
        ResponseEntity<AuthResponseDto> response = authController.login(loginRequest);

        // Assert
        assertEquals(500, response.getStatusCode().value());

        // Optional: Verify that the code stopped after the exception and didn't try to find the user
        verify(userRepository, never()).findByPhoneNumber(anyString());
    }
}