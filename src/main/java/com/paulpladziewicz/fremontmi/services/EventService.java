package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.EventRepository;
import com.paulpladziewicz.fremontmi.repositories.UserDetailsRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;

    private final UserService userService;

    private final UserDetailsRepository userDetailsRepository;

    public EventService(EventRepository eventRepository, UserService userService, UserDetailsRepository userDetailsRepository) {
        this.eventRepository = eventRepository;
        this.userService = userService;
        this.userDetailsRepository = userDetailsRepository;
    }

    public List<Event> findAll() {
        return eventRepository.findAll();
    }

    public Event findEventById(String id) {
        return eventRepository.findById(id).orElse(null);
    }

    public List<EventDetailsDto> findEventsByUser() {
        UserDetailsDto userDetails = userService.getUserDetails();

        List<Event> events = eventRepository.findAllById(userDetails.getEventAdminIds());

        return events.stream()
                .map(event -> {
                    EventDetailsDto dto = new EventDetailsDto();
                    dto.setEvent(event);
                    if (userDetails.getEventAdminIds().contains(event.getId())) {
                        dto.setUserRole("admin");
                    } else {
                        dto.setUserRole("member");
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Event createEvent(Event event) {
        UserDetailsDto userDetails = userService.getUserDetails();
        var username = userDetails.getUsername();

        event.setOrganizerId(username);
        var savedEvent = eventRepository.save(event);

        List<String> eventAdminIds = new ArrayList<>(userDetails.getEventAdminIds());
        eventAdminIds.add(savedEvent.getId());
        userDetails.setEventAdminIds(eventAdminIds);
        userDetailsRepository.save(userDetails);

        return savedEvent;
    }

    public void deleteEvent (String groupId) {
        eventRepository.deleteById(groupId);
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
