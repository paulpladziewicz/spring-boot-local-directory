package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class NeighborService {
    @Id
    private String id;

    @NotNull(message = "Group name must not be null")
    @Size(min = 3, max = 100, message = "Group name must be between 3 and 100 characters")
    private String name;

    @NotNull(message = "Description must not be null")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    private String imageUrl;

    @Indexed
    private List<String> category;

    @Indexed
    private List<String> tags;

    private String status = "active";

    private String visibility = "public";

    private List<Announcement> announcements = new ArrayList<>();

    private String organizerId;

    @NotBlank(message = "Contact name is required")
    private String contactName;

    @Email(message = "Invalid email format")
    private String contactEmail;

    @Pattern(regexp = "^\\+?\\d{1,15}$", message = "Invalid phone number")
    private String contactPhone;

    private Date creationDate = new Date();
}
