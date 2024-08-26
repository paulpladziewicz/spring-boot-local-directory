package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Data
public class Announcement {

    private int id;

    @NotBlank(message = "Title is required.")
    @Size(max = 256, message = "Title should be less than 256 characters.")
    private String title;

    @NotBlank(message = "Content is required.")
    @Size(max = 2000, message = "Content should be less than 2000 characters.")
    private String content;

    private Instant creationDate;

    public String getTimeAgo() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime creationTime = creationDate.atZone(ZoneId.systemDefault());

        long seconds = ChronoUnit.SECONDS.between(creationTime, now);

        if (seconds < 60) {
            return seconds + " seconds ago";
        }

        long minutes = ChronoUnit.MINUTES.between(creationTime, now);
        if (minutes < 60) {
            return minutes + " minutes ago";
        }

        long hours = ChronoUnit.HOURS.between(creationTime, now);
        if (hours < 24) {
            return hours + " hours ago";
        }

        long days = ChronoUnit.DAYS.between(creationTime, now);
        return days + " days ago";
    }
}
