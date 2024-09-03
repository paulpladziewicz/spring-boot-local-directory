package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.EventRepository;
import com.paulpladziewicz.fremontmi.repositories.UserProfileRepository;
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

    private final UserProfileRepository userProfileRepository;

    public EventService(EventRepository eventRepository, UserService userService, UserProfileRepository userProfileRepository) {
        this.eventRepository = eventRepository;
        this.userService = userService;
        this.userProfileRepository = userProfileRepository;
    }

    public List<Event> findAll() {
        LocalDateTime now = LocalDateTime.now();

        return eventRepository.findBySoonestStartTimeAfterOrderBySoonestStartTimeAsc(now);
    }

    public Event findEventById(String id) {
        return eventRepository.findById(id).orElse(null);
    }

    public List<Event> findEventsByUser() {
        UserProfile userDetails = userService.getUserProfile();

        return eventRepository.findAllById(userDetails.getEventAdminIds());
    }

    public Event createEvent(Event event) {
        UserProfile userDetails = userService.getUserProfile();
        var userId = userDetails.getUserId();

        // Find the soonest start time
        LocalDateTime soonestStartTime = event.getDays().stream()
                .map(DayEvent::getStartTime)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        event.setSoonestStartTime(soonestStartTime);

        // Optionally handle the end time logic if needed
        event.getDays().forEach(dayEvent -> {
            if (dayEvent.getEndTime() != null && dayEvent.getEndTime().isBefore(dayEvent.getStartTime())) {
                throw new IllegalArgumentException("End time(s) must be after the start time.");
            }
        });

        event.setOrganizerId(userId);
        populateFormattedTimes(event);
        var savedEvent = eventRepository.save(event);

        List<String> eventAdminIds = new ArrayList<>(userDetails.getEventAdminIds());
        eventAdminIds.add(savedEvent.getId());
        userDetails.setEventAdminIds(eventAdminIds);
        userProfileRepository.save(userDetails);

        return savedEvent;
    }

    public void updateEvent(String id, Event updatedEvent) {
        // Fetch the existing event from the repository
        Event existingEvent = eventRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Event not found")
        );

        // Check if the current user is allowed to update the event
        UserProfile userDetails = userService.getUserProfile();
        if (!userDetails.getEventAdminIds().contains(id)) {
            throw new RuntimeException("User doesn't have permission to update this event");
        }

        // Update the event's fields with the new data
        existingEvent.setName(updatedEvent.getName());
        existingEvent.setDescription(updatedEvent.getDescription());
        existingEvent.setLocationName(updatedEvent.getLocationName());
        existingEvent.setAddress(updatedEvent.getAddress());
        existingEvent.setDays(updatedEvent.getDays());

        // Find the soonest start time in the updated days
        LocalDateTime soonestStartTime = updatedEvent.getDays().stream()
                .map(DayEvent::getStartTime)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        existingEvent.setSoonestStartTime(soonestStartTime);

        // Optionally handle the end time logic
        updatedEvent.getDays().forEach(dayEvent -> {
            if (dayEvent.getEndTime() != null && dayEvent.getEndTime().isBefore(dayEvent.getStartTime())) {
                throw new IllegalArgumentException("End time(s) must be after the start time.");
            }
        });

        // Populate formatted times for display purposes
        populateFormattedTimes(existingEvent);

        // Save the updated event to the repository
        eventRepository.save(existingEvent);
    }

    public void cancelEvent(String eventId) {
        Event event = findEventById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found");
        }
        event.setStatus("cancelled");
        eventRepository.save(event);
    }

    public void reactivateEvent(String eventId) {
        Event event = findEventById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found");
        }
        event.setStatus("active");
        eventRepository.save(event);
    }

    public void deleteEvent (String eventId) {
        UserProfile userDetails = userService.getUserProfile();
        if (!userDetails.getEventAdminIds().contains(eventId)) {
            throw new RuntimeException("User doesn't have permission to delete this event");
        }
        eventRepository.deleteById(eventId);
    }

    public void populateFormattedTimes(Event event) {
        List<String> formattedTimes = event.getDays().stream()
                .flatMap(dayEvent -> {
                    String formattedStartTime = formatDateTime(dayEvent.getStartTime());
                    String formattedEndTime = dayEvent.getEndTime() != null
                            ? formatDateTime(dayEvent.getEndTime())
                            : "No End Time"; // or use "N/A" or any other placeholder
                    return Stream.of(formattedStartTime, formattedEndTime);
                })
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
