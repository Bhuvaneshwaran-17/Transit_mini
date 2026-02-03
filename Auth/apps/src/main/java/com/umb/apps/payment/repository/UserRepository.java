package com.umb.apps.payment.repository;

import com.umb.apps.payment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUserName(String userName);
    boolean existsByPhoneNumber(String phoneNumber);

    // Add this line - we need the object, not just a boolean!
    Optional<User> findByPhoneNumber(String phoneNumber);
}