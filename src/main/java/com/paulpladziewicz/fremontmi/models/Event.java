package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@TypeAlias("Event")
public class Event implements ContentDetail {

    @NotBlank(message = "Please provide an event name")
    @Size(min = 3, max = 100, message = "Event name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Please provide a description")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    private List<String> tags = new ArrayList<>();

    @NotBlank(message = "Please provide a location name")
    @Size(max = 256, message = "Location name can't be longer than 256 characters")
    private String locationName;

    @NotBlank(message = "Please provide an address")
    @Size(max = 256, message = "Address can't be longer than 256 characters")
    private String address;

    @Indexed
    private LocalDateTime soonestStartTime;

    @NotEmpty(message = "Event must have at least one date and time.")
    @Valid
    private List<DayEvent> days;

    private List<String> formattedTimes;

    @Transient
    private DayEvent nextAvailableDayEvent;

    @Transient
    private int moreDayEventsCount;

    @Override
    public void update(ContentDetail newDetail, Content parentContent) {
    }

    @Override
    public void update(UpdateType updateType, Map<String, Object> updateData) {
    }
}

