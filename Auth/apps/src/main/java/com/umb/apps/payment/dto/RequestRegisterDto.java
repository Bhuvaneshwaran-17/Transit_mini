package com.umb.apps.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RequestRegisterDto {
@NotBlank(message = "username should not be empty")
    private String userName;

@NotBlank(message = "phone number should not be empty")
@Pattern(regexp = "^[0-9]{10}$", message = "phone number must contain only digits")
@Size(min=10, max=10, message = "phone number must be 10 digits")
    private String phoneNumber;

@NotBlank(message = "password should not be empty")
@Size(min=6, max=12, message = "password must be 6-12 characters")
    private String password;

@NotBlank(message = "confirm password must not be empty")
    private String confirmPassword;

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
