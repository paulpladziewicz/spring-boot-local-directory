package com.paulpladziewicz.fremontmi.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendEmailDto {
    @NotBlank(message = "Please provide an email subject.")
    private String subject;

    @NotBlank(message = "Please provide a message.")
    private String message;
}
