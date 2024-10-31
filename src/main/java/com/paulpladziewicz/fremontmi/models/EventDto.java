package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EventDto implements ContentDto {
    private String contentId;

    private ContentStatus status;

    private boolean nearby;

    private String pathname;

    private List<String> tags = new ArrayList<>();

    @NotBlank(message = "Please provide an event name")
    @Size(min = 3, max = 100, message = "Event name must be between 3 and 100 characters")
    private String title;

    @NotBlank(message = "Please provide a description")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    @NotBlank(message = "Please provide a location name")
    @Size(max = 256, message = "Location name can't be longer than 256 characters")
    private String locationName;

    @NotBlank(message = "Please provide an address")
    @Size(max = 256, message = "Address can't be longer than 256 characters")
    private String address;

    @NotEmpty(message = "Event must have at least one date and time.")
    @Valid
    private List<DayEvent> days;
}
