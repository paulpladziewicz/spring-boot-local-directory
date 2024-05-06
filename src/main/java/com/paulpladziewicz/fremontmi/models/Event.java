package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    private String locationName;

    private String address;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime registrationStart;

    private LocalDateTime registrationEnd;

    private Double cost;

    private String status = "active";

    private String visibility = "public";

    private String joinPolicy = "open";

    private String organizerId;

    private String contactName;

    private String contactEmail;

    private String contactPhone;
}

