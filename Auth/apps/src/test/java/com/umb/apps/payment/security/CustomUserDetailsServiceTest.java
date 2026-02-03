package com.umb.apps.payment.security;

import com.umb.apps.payment.entity.User;
import com.umb.apps.payment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_WithPhoneNumber_Success() {
        // Arrange: Input length >= 10 triggers phone search
        String phoneNumber = "9876543210";
        User mockUser = new User();
        mockUser.setPhoneNumber(phoneNumber);
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(mockUser));

        // Act
        UserDetails result = customUserDetailsService.loadUserByUsername(phoneNumber);

        // Assert
        assertNotNull(result);
        verify(userRepository).findByPhoneNumber(phoneNumber);
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void loadUserByUsername_WithId_Success() {
        // Arrange: Input length < 10 triggers ID search
        String userIdStr = "123";
        User mockUser = new User();
        mockUser.setId(123L);
        when(userRepository.findById(123L)).thenReturn(Optional.of(mockUser));

        // Act
        UserDetails result = customUserDetailsService.loadUserByUsername(userIdStr);

        // Assert
        assertNotNull(result);
        verify(userRepository).findById(123L);
    }

    @Test
    void loadUserByUsername_PhoneNotFound_ShouldThrowException() {
        String phoneNumber = "111222333444";
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(phoneNumber));
    }

    @Test
    void loadUserByUsername_InvalidFormat_ShouldThrowException() {
        // Arrange: Short string that isn't a number
        String invalidInput = "abc";

        // Act & Assert
        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(invalidInput));

        assertEquals("Invalid identity format", ex.getMessage());
    }
}