package com.umb.apps.payment.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("Test Getters, Setters, and Lombok Data features")
    void testUserDataProperties() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUserName("bhuvi_dev");
        user1.setPhoneNumber("1234567890");
        user1.setPasswordHash("hashed_pw");

        // Getter tests
        assertThat(user1.getId()).isEqualTo(1L);
        assertThat(user1.getUsername()).isEqualTo("bhuvi_dev");
        assertThat(user1.getPhoneNumber()).isEqualTo("1234567890");
        assertThat(user1.getPasswordHash()).isEqualTo("hashed_pw");

        // Equals and HashCode tests
        User user2 = new User();
        user2.setId(1L);
        user2.setUserName("bhuvi_dev");
        user2.setPhoneNumber("1234567890");
        user2.setPasswordHash("hashed_pw");

        assertThat(user1).isEqualTo(user2);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());

        // ToString test
        assertThat(user1.toString()).contains("userName=bhuvi_dev");
    }

    @Test
    @DisplayName("Test Spring Security UserDetails Implementation")
    void testUserDetailsMethods() {
        User user = new User();
        user.setUserName("authority_test");
        user.setPasswordHash("secret");

        // Authorities should be empty as per your implementation
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertThat(authorities).isEmpty();

        // Mapping checks
        assertThat(user.getUsername()).isEqualTo("authority_test");
        assertThat(user.getPassword()).isEqualTo("secret");

        // Status checks (Uncomment these in your Entity first)
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }
}