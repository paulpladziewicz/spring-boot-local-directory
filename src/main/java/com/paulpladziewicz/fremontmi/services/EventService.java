package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.EventRepository;
import com.paulpladziewicz.fremontmi.repositories.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final UserService userService;
    private final UserProfileRepository userProfileRepository;

    public EventService(EventRepository eventRepository, UserService userService, UserProfileRepository userProfileRepository) {
        this.eventRepository = eventRepository;
        this.userService = userService;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public ServiceResponse<Event> createEvent(Event event) {
        try {
            Optional<UserProfile> userProfileOpt = userService.getUserProfile();
            if (userProfileOpt.isEmpty()) {
                return logAndReturnError("Failed to create event: user profile not found.", "user_profile_not_found");
            }

            UserProfile userDetails = userProfileOpt.get();
            String userId = userDetails.getUserId();

            validateEventTimes(event);

            event.setOrganizerId(userId);
            populateFormattedTimes(event);

            Event savedEvent = eventRepository.save(event);

            userDetails.getEventAdminIds().add(savedEvent.getId());
            userProfileRepository.save(userDetails);

            return ServiceResponse.value(savedEvent);
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while creating event.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while creating event.", "unexpected_error", e);
        }
    }

    public ServiceResponse<List<Event>> findAll() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Event> events = eventRepository.findBySoonestStartTimeAfterOrderBySoonestStartTimeAsc(now);
            return ServiceResponse.value(events);
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while retrieving events.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while retrieving events.", "unexpected_error", e);
        }
    }

    public ServiceResponse<Event> findEventById(String id) {
        try {
            return eventRepository.findById(id)
                    .map(ServiceResponse::value)
                    .orElseGet(() -> logAndReturnError("Event not found with id: " + id, "event_not_found"));
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while retrieving event.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while retrieving event.", "unexpected_error", e);
        }
    }

    public ServiceResponse<List<Event>> findEventsByUser() {
        try {
            Optional<UserProfile> userProfileOpt = userService.getUserProfile();
            if (userProfileOpt.isEmpty()) {
                return logAndReturnError("Failed to retrieve events: user profile not found.", "user_profile_not_found");
            }

            UserProfile userDetails = userProfileOpt.get();
            List<Event> events = eventRepository.findAllById(userDetails.getEventAdminIds());
            return ServiceResponse.value(events);
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while retrieving events.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while retrieving events.", "unexpected_error", e);
        }
    }

    @Transactional
    public ServiceResponse<Void> updateEvent(String id, Event updatedEvent) {
        try {
            Event existingEvent = eventRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found"));

            ServiceResponse<UserProfile> userProfileResult = validateUserProfileForEventAdmin(id);
            if (userProfileResult.hasError()) {
                return ServiceResponse.error(userProfileResult.errorCode());
            }

            validateEventTimes(updatedEvent);

            updateExistingEvent(existingEvent, updatedEvent);
            eventRepository.save(existingEvent);

            return ServiceResponse.value(null);
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while updating event.", "database_error", e);
        } catch (IllegalArgumentException e) {
            return logAndReturnError("Invalid argument while updating event: " + e.getMessage(), "invalid_argument");
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while updating event.", "unexpected_error", e);
        }
    }

    @Transactional
    public ServiceResponse<Void> deleteEvent(String eventId) {
        try {
            ServiceResponse<UserProfile> userProfileResult = validateUserProfileForEventAdmin(eventId);
            if (userProfileResult.hasError()) {
                return ServiceResponse.error(userProfileResult.errorCode());
            }

            eventRepository.deleteById(eventId);
            logger.info("Successfully deleted event with id: {}", eventId);
            return ServiceResponse.value(null);
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while deleting event.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while deleting event.", "unexpected_error", e);
        }
    }

    public ServiceResponse<Void> cancelEvent(String eventId) {
        return updateEventStatus(eventId, "cancelled", "Failed to cancel event.");
    }

    public ServiceResponse<Void> reactivateEvent(String eventId) {
        return updateEventStatus(eventId, "active", "Failed to reactivate event.");
    }

    private ServiceResponse<Void> updateEventStatus(String eventId, String status, String errorMessage) {
        try {
            ServiceResponse<Event> eventResult = findEventById(eventId);
            if (eventResult.hasError()) {
                return logAndReturnError(errorMessage, eventResult.errorCode());
            }

            Event event = eventResult.value();
            event.setStatus(status);
            eventRepository.save(event);

            logger.info("Event with id {} was successfully updated to status: {}", eventId, status);
            return ServiceResponse.value(null);
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while updating event status.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while updating event status.", "unexpected_error", e);
        }
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

    private ServiceResponse<UserProfile> validateUserProfileForEventAdmin(String eventId) {
        Optional<UserProfile> userProfileOpt = userService.getUserProfile();
        if (userProfileOpt.isEmpty()) {
            return logAndReturnError("User profile not found.", "user_profile_not_found");
        }

        UserProfile userProfile = userProfileOpt.get();
        if (!userProfile.getEventAdminIds().contains(eventId)) {
            return logAndReturnError("User does not have permission for event: " + eventId, "permission_denied");
        }

        return ServiceResponse.value(userProfile);
    }

    private void updateExistingEvent(Event existingEvent, Event updatedEvent) {
        existingEvent.setName(updatedEvent.getName());
        existingEvent.setDescription(updatedEvent.getDescription());
        existingEvent.setLocationName(updatedEvent.getLocationName());
        existingEvent.setAddress(updatedEvent.getAddress());
        existingEvent.setDays(updatedEvent.getDays());
        populateFormattedTimes(existingEvent);
    }

    public void populateFormattedTimes(Event event) {
        List<String> formattedTimes = event.getDays().stream()
                .flatMap(dayEvent -> Stream.of(
                        formatDateTime(dayEvent.getStartTime()),
                        dayEvent.getEndTime() != null ? formatDateTime(dayEvent.getEndTime()) : "No End Time"
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
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }

    private <T> ServiceResponse<T> logAndReturnError(String message, String errorCode) {
        logger.error(message);
        return ServiceResponse.error(errorCode);
    }

    private <T> ServiceResponse<T> logAndReturnError(String message, String errorCode, Exception e) {
        logger.error(message, e);
        return ServiceResponse.error(errorCode);
    }
}