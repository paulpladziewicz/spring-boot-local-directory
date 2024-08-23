package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Event;
import com.paulpladziewicz.fremontmi.repositories.EventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> findAll() {
        return eventRepository.findAll();
    }

    public Event findEventById(String id) {
        return eventRepository.findById(id).orElse(null);
    }

    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }

    protected String formatAsEasternTime(Instant utcTime) {
        ZonedDateTime easternTime = utcTime.atZone(ZoneId.of("America/New_York"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a z");
        return easternTime.format(formatter);
    }

    protected void formatAsSimpleTime(Instant utcTime) {
        Instant now = Instant.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
        String simplifiedTime = formatter.format(now);
        System.out.println("Simplified Time: " + simplifiedTime);
    }
}
