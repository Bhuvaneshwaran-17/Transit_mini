
// src/main/java/com/umb/apps/payment/service/UserService.java
package com.umb.apps.payment.service;

import com.umb.apps.payment.dto.RequestRegisterDto;
import com.umb.apps.payment.entity.User;
import com.umb.apps.payment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repo;
    // BCryptPasswordEncoder is thread-safe; reuse a single instance
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();


    @Transactional
    public Long register(RequestRegisterDto dto) {
        // 1) Cross-field validation
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // 2) Uniqueness checks (pre-checks to give friendly messages)
        if (repo.existsByUserName(dto.getUserName())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (repo.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        // 3) Map DTO → Entity & hash password
        User user = new User();
        user.setUserName(dto.getUserName().trim());
        user.setPhoneNumber(dto.getPhoneNumber().trim());
        user.setPasswordHash(encoder.encode(dto.getPassword()));

        // 4) Persist
        try {
            User saved = repo.save(user);
            return saved.getId(); // return ID for controller
        } catch (DataIntegrityViolationException ex) {
            // In case of race conditions, DB unique constraints still win.
            // Your GlobalExceptionHandler already maps this to 409 with "User already exists".
            throw ex;
        }
    }

}
