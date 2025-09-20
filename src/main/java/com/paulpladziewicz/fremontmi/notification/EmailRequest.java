package com.paulpladziewicz.fremontmi.notification;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class EmailRequest {
    @NotBlank
    private String contentId;

    @NotBlank
    private String subject;

    @NotBlank
    private String message;
}
