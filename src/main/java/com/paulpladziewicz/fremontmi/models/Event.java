package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=true)
@TypeAlias("Event")
public class Event extends Content {

    @NotBlank(message = "Event name must not be null")
    @Size(min = 3, max = 100, message = "Event name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Description must not be null")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    private List<String> tags = new ArrayList<>();

    @Size(max = 256, message = "Location name can't be longer than 256 characters")
    private String locationName;

    private String address;

    @Indexed
    private LocalDateTime soonestStartTime;

    @NotEmpty(message = "Event must have at least one date and time.")
    private List<DayEvent> days;

    private List<String> formattedTimes;

    @Transient
    private DayEvent nextAvailableDayEvent;

    @Transient
    private int moreDayEventsCount;
}

