package com.paulpladziewicz.fremontmi.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class AnnouncementDto {
    private int id;

    private String contentId;

    @NotBlank(message = "Title is required.")
    @Size(max = 256, message = "Title should be less than 256 characters.")
    private String title;

    @NotBlank(message = "Content is required.")
    @Size(max = 2000, message = "Content should be less than 2000 characters.")
    private String message;

    private Instant createdAt;
}
