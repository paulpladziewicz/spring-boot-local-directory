package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "events")
public class Event {

    @Id
    private String id;

    @NotBlank(message = "Event name must not be null")
    @Size(min = 3, max = 100, message = "Event name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Description must not be null")
    @Size(max = 500, message = "Description can't be longer than 500 characters")
    private String description;

    private List<String> tags;

    @Size(max = 256, message = "Location name can't be longer than 256 characters")
    private String locationName;

    private String address;

    private List<DayEvent> days;

    @NotNull(message = "Registration start time cannot be null")
    @Future(message = "Registration start time must be in the future")
    private LocalDateTime registrationStartTime;

    private LocalDateTime registrationEndTime;

    @NotNull(message = "Registration end time cannot be null")
    @Future(message = "Registration end time must be in the future")
    @AssertTrue(message = "Registration end time must be after registration start time")
    private boolean isRegistrationEndTimeValid() {
        return registrationEndTime.isAfter(registrationStartTime);
    }

    @PositiveOrZero(message = "Cost must be non-negative")
    private Double cost;

    private String status = "active";

    private String visibility = "public";

    private String joinPolicy = "open";

    private String organizerId;

    @NotBlank(message = "Contact name is required")
    private String contactName;

    @Email(message = "Invalid email format")
    private String contactEmail;

    @Pattern(regexp = "^\\+?\\d{1,15}$", message = "Invalid phone number")
    private String contactPhone;
}

