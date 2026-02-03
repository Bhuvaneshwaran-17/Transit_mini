package com.umb.apps.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileDto {
    private String userName;
    private String phoneNumber;
    // Add email or other fields if needed
}
