package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class Event implements ContentDetail {

    @NotBlank(message = "Please provide an event name")
    @Size(min = 3, max = 100, message = "Event name must be between 3 and 100 characters")
    private String title;

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
    public void update(Content parentContent, ContentDto newDetail) {
        if (!(newDetail instanceof Event newEventDetail)) {
            throw new IllegalArgumentException("Invalid content detail type for Business.");
        }

        this.setTitle(newEventDetail.getTitle());
        this.setDescription(newEventDetail.getDescription());
        this.setLocationName(newEventDetail.getLocationName());
        this.setAddress(newEventDetail.getAddress());
        this.setDays(newEventDetail.getDays());

        populateFormattedTimes(this);
    }

    private void validateEventTimes(Event event) {
        LocalDateTime soonestStartTime = event.getDays().stream()
                .map(DayEvent::getStartTime)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        event.setSoonestStartTime(soonestStartTime);

        event.getDays().forEach(dayEvent -> {
            if (dayEvent.getEndTime() != null && dayEvent.getEndTime().isBefore(dayEvent.getStartTime())) {
                throw new IllegalArgumentException("End time(s) must be after the start time.");
            }
        });
    }

    public void populateFormattedTimes(Event event) {
        List<String> formattedTimes = event.getDays().stream()
                .flatMap(dayEvent -> Stream.of(
                        formatDateTime(dayEvent.getStartTime()),
                        formatDateTime(dayEvent.getEndTime())
                ))
                .collect(Collectors.toList());

        event.setFormattedTimes(formattedTimes);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "No End Time";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d h:mm a");
        String formattedDate = dateTime.format(formatter);
        int day = dateTime.getDayOfMonth();
        String suffix = getDayOfMonthSuffix(day);
        return formattedDate.replaceFirst("\\d+", day + suffix);
    }

    private String getDayOfMonthSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}

