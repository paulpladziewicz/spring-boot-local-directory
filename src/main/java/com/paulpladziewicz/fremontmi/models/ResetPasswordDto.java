package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

@Data
public class ResetPasswordDto {
    private String password;
    private String confirmPassword;
    private String token;
}

