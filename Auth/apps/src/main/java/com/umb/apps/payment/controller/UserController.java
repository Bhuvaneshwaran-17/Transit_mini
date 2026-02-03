
package com.umb.apps.payment.controller;

import com.umb.apps.payment.dto.RequestRegisterDto;
import com.umb.apps.payment.dto.UserProfileDto;
import com.umb.apps.payment.repository.UserRepository;
import com.umb.apps.payment.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;


    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(@RequestBody @Valid RequestRegisterDto dto) {
        Long userId = userService.register(dto); // return created user's ID from service

        Map<String, Object> body = Map.of(
                "message", "User created successfully",
                "userId", userId
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        // Principal is now just the ID String
        Long userId = Long.valueOf(authentication.getName());

        // Only query the DB when the user actually wants to see their profile
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(new UserProfileDto(
                        user.getUsername(),
                        user.getPhoneNumber()
                )))
                .orElse(ResponseEntity.status(404).build());
    }
}

