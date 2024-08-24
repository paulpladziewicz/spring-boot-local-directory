package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class DayEvent {

    @NotNull(message = "Event start time cannot be null")
    @DateTimeFormat(pattern = "yyyy-MM-dd h:mm a")
    @Future(message = "Event start time must be in the future")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd h:mm a")
    private LocalDateTime endTime;
}

