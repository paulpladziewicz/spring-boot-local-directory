package com.paulpladziewicz.fremontmi.content;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NeighborServicesProfileDto implements ContentDto {

    private String contentId;

    private String pathname;

    private boolean external;

    private boolean nearby;

    private List<String> tags = new ArrayList<>();

    @NotBlank(message = "Display name is required.")
    @Size(max = 100, message = "Display name should be less than 100 characters.")
    private String title;

    @NotBlank(message = "Description should not be blank.")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format.")
    @Size(max = 100, message = "Email must not be longer than 100 characters.")
    private String email;

    private List<NeighborService> neighborServices = new ArrayList<>();

    private String profileImageUrl;

    private String profileImageFileName;
}
