package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DayEvent {
    @NotNull(message = "Event start time cannot be null")
    @Future(message = "Event start time must be in the future")
    private LocalDateTime eventStartTime;

    private LocalDateTime eventEndTime;

    @NotNull(message = "End time cannot be null")
    private LocalDateTime endTime;

    @AssertTrue(message = "End time must be after start time")
    private boolean isEndTimeValid() {
        return endTime.isAfter(eventStartTime);
    }

    @NotNull(message = "Event end time cannot be null")
    @Future(message = "Event end time must be in the future")
    @AssertTrue(message = "Event end time must be after event start time")
    private boolean isEventEndTimeValid() {
        return eventEndTime.isAfter(eventStartTime);
    }
}

