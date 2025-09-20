package com.paulpladziewicz.fremontmi.user;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserRegistrationDto {

    @NotBlank(message = "First name is required.")
    @Size(max = 50, message = "First name should be less than 50 characters.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(max = 50, message = "Last name should be less than 50 characters.")
    private String lastName;

    @NotBlank(message = "Username is required.")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters long.")
    @Pattern(regexp = "^[^@\\s]+$", message = "Username must not be an email address and cannot contain spaces.")
    private String username;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format.")
    @Size(max = 100, message = "Email must not be longer than 100 characters.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password must be at least 8 characters long.")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).*$",
            message = "Password must contain at least one digit, one letter, and one special character.")
    private String password;

    @NotBlank(message = "Matching password is required.")
    private String matchingPassword;

    @AssertTrue(message = "You must accept the terms.")
    private boolean termsAccepted;

    private LocalDateTime termsAcceptedAt;
}
