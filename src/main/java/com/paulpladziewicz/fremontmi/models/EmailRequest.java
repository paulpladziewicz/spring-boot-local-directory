package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmailRequest {
    @NotNull
    private String contentId;

    @NotBlank
    private String subject;

    @NotBlank
    private String message;
}
