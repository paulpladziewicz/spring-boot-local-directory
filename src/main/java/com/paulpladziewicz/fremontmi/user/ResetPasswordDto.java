package com.paulpladziewicz.fremontmi.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDto {

    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password must be at least 8 characters long.")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).*$",
            message = "Password must contain at least one digit, one letter, and one special character.")
    private String password;

    @NotBlank(message = "Matching password is required.")
    private String confirmPassword;
    private String token;
}

