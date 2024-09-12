package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "neighbor_services_profiles")
public class NeighborServiceProfile {

    @Id
    private String id;

    @NotBlank(message = "First name is required.")
    @Size(max = 50, message = "First name should be less than 50 characters.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(max = 50, message = "Last name should be less than 50 characters.")
    private String lastName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format.")
    @Size(max = 100, message = "Email must not be longer than 100 characters.")
    private String email;

    @NotBlank(message = "Description must not be null")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    @Indexed
    private List<String> tags = new ArrayList<>();

    private List<NeighborService> neighborServices = new ArrayList<>();

    private String status = "incomplete";

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}