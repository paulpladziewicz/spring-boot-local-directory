package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DayEvent {

    @NotNull(message = "Event start time cannot be null")
    @Future(message = "Event start time must be in the future")
    private LocalDateTime eventStartTime;

    @Future(message = "Event end time must be in the future")
    private LocalDateTime eventEndTime;
}

