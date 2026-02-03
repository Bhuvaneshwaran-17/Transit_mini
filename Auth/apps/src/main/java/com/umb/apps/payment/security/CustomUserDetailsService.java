package com.umb.apps.payment.security;

import com.umb.apps.payment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        // 1. If the input is long (like a phone number), search the phone_number column
        if (input.length() >= 10) {
            return userRepository.findByPhoneNumber(input)
                    .map(user -> (UserDetails) user)
                    .orElseThrow(() -> new UsernameNotFoundException("Phone number not found"));
        }

        // 2. If the input is short (like an ID from a JWT), search the ID column
        try {
            Long id = Long.valueOf(input);
            return userRepository.findById(id)
                    .map(user -> (UserDetails) user)
                    .orElseThrow(() -> new UsernameNotFoundException("User ID not found"));
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("Invalid identity format");
        }
    }
}