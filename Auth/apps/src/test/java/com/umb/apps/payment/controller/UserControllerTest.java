package com.umb.apps.payment.controller;


import com.umb.apps.payment.dto.RequestRegisterDto;
import com.umb.apps.payment.entity.User;
import com.umb.apps.payment.repository.UserRepository;
import com.umb.apps.payment.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserController userController;

    @Test
    void registerUser_Success_ShouldReturn201() {
        // Arrange
        RequestRegisterDto dto = new RequestRegisterDto(); // Assumes no-args constructor
        when(userService.register(dto)).thenReturn(1L);

        // Act
        ResponseEntity<Map<String, Object>> response = userController.registerUser(dto);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1L, response.getBody().get("userId"));
        assertEquals("User created successfully", response.getBody().get("message"));
    }

    @Test
    void getUserProfile_Success_ShouldReturnProfile() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("1"); // Mocking the User ID string from JWT

        User mockUser = new User();
        mockUser.setUserName("Bhuvi");
        mockUser.setPhoneNumber("9876543210");

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // Act
        ResponseEntity<?> response = userController.getUserProfile(auth);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getUserProfile_UserNotFound_ShouldReturn404() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("99"); // ID that doesn't exist

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = userController.getUserProfile(auth);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}