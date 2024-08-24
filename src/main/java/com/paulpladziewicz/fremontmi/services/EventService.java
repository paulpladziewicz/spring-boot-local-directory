package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.EventRepository;
import com.paulpladziewicz.fremontmi.repositories.UserDetailsRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        populateFormattedTimes(event);
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
        switch (day % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
    }
}
