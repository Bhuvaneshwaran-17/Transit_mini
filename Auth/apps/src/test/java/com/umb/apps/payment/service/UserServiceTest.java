package com.umb.apps.payment.service;

import com.umb.apps.payment.dto.RequestRegisterDto;
import com.umb.apps.payment.entity.User;
import com.umb.apps.payment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repo;

    @InjectMocks
    private UserService userService;

    private RequestRegisterDto validDto;

    @BeforeEach
    void setUp() {
        validDto = new RequestRegisterDto();
        validDto.setUserName("Bhuvi");
        validDto.setPhoneNumber("9876543210");
        validDto.setPassword("Pass123");
        validDto.setConfirmPassword("Pass123");
    }

    @Test
    void register_Success_ShouldReturnUserId() {
        // Arrange
        User mockSavedUser = new User();
        mockSavedUser.setId(100L);

        when(repo.existsByUserName(anyString())).thenReturn(false);
        when(repo.existsByPhoneNumber(anyString())).thenReturn(false);
        when(repo.save(any(User.class))).thenReturn(mockSavedUser);

        // Act
        Long userId = userService.register(validDto);

        // Assert
        assertEquals(100L, userId);
        verify(repo, times(1)).save(any(User.class));
    }

    @Test
    void register_PasswordsDoNotMatch_ShouldThrowException() {
        // Arrange
        validDto.setConfirmPassword("WrongPass");

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register(validDto));

        assertEquals("Passwords do not match", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void register_UsernameExists_ShouldThrowException() {
        // Arrange
        when(repo.existsByUserName("Bhuvi")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register(validDto));

        assertEquals("Username already exists", ex.getMessage());
    }

    @Test
    void register_PhoneExists_ShouldThrowException() {
        // Arrange
        when(repo.existsByUserName(anyString())).thenReturn(false);
        when(repo.existsByPhoneNumber("9876543210")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register(validDto));

        assertEquals("Phone number already registered", ex.getMessage());
    }
}