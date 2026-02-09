package com.umb.apps.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User implements UserDetails { // <--- YOU MUST ADD THIS

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name", nullable = false, unique = true, length = 50)
    private String userName;

    @Column(name = "phone_number", nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    // --- REQUIRED BY USERDETAILS INTERFACE ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Return empty list if you don't have roles/permissions yet
        return List.of();
    }

    @Override
    public String getPassword() {
        return this.passwordHash; // Maps your custom column to Spring's requirement
    }

    @Override
    public String getUsername() {
        return this.userName; // Maps your custom column to Spring's requirement
    }


}