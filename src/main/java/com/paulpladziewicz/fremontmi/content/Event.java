package com.paulpladziewicz.fremontmi.content;

import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class Event implements ContentDetail {

    private String title;

    private String description;

    private String locationName;

    private String address;

    private List<DayEvent> days;

    @Transient
    private DayEvent nextAvailableDayEvent;

    @Transient
    private int availableDayEventCount;

    private List<String> formattedTimes;

    private String externalUrl;

    private Map<String, Object> images;

    @Override
    public void update(Content content, ContentDto contentDto) {
        if (!(contentDto instanceof EventDto event)) {
            throw new IllegalArgumentException("Invalid content detail type for Business.");
        }

        setTitle(event.getTitle());
        setDescription(event.getDescription());
        setLocationName(event.getLocationName());
        setAddress(event.getAddress());
        setDays(event.getDays());
        validateEventTimes(days);
        setExternalUrl(event.getExternalUrl());

        populateFormattedTimes(this);
    }

    public void validateEventTimes(List<DayEvent> days) {
        for (int i = 0; i < days.size(); i++) {
            DayEvent dayEvent = days.get(i);
            if (dayEvent.getEndTime() != null && dayEvent.getEndTime().isBefore(dayEvent.getStartTime())) {
                throw new IllegalArgumentException("End time must be after start time for event date #" + (i + 1));
            }
        }
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

