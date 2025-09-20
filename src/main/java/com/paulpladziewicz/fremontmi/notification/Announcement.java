package com.paulpladziewicz.fremontmi.notification;

import lombok.Data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Data
public class Announcement {

    private int id;

    private String title;

    private String message;

    private Instant createdAt;

    public String getTimeAgo() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime creationTime = createdAt.atZone(ZoneId.systemDefault());

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
