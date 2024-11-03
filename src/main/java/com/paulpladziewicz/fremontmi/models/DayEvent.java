package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class DayEvent {

    @NotNull(message = "Please provide a start date and time")
    @DateTimeFormat(pattern = "yyyy-MM-dd h:mm a")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd h:mm a")
    private LocalDateTime endTime;
}

