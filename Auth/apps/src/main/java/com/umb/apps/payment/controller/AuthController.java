package com.umb.apps.payment.controller;

import com.umb.apps.payment.dto.AuthResponseDto;
import com.umb.apps.payment.dto.LoginRequestDto;
import com.umb.apps.payment.repository.UserRepository;
import com.umb.apps.payment.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody @Valid LoginRequestDto req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getPhoneNumber(), req.getPassword())
            );

            return userRepository.findByPhoneNumber(req.getPhoneNumber())
                    .map(user -> {
                        String token = jwtService.generateToken(String.valueOf(user.getId()));
                        log.info("Token generated for user ID: {}", user.getId()); // 3. Use log.info
                        return ResponseEntity.ok(new AuthResponseDto(token, "Bearer"));
                    })
                    .orElseGet(() -> {
                        log.warn("User authenticated but not found in DB: {}", req.getPhoneNumber());
                        return ResponseEntity.status(401).build();
                    });

        } catch (BadCredentialsException ex) {
            log.error("Login failed - Bad Credentials for phone: {}", req.getPhoneNumber()); // 4. Use log.error
            return ResponseEntity.status(401).body(new AuthResponseDto(null, "Bearer"));
        } catch (Exception e) {
            log.error("Unexpected login error: ", e); // 5. Log the actual exception stack trace
            return ResponseEntity.status(500).build();
        }
    }
}