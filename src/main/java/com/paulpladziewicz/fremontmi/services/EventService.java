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
    public ServiceResult<Event> createEvent(Event event) {
        try {
            Optional<UserProfile> userProfileOpt = userService.getUserProfile();
            if (userProfileOpt.isEmpty()) {
                logger.error("Failed to create event: could not retrieve user profile.");
                return ServiceResult.error("Failed to create event: could not retrieve user profile.", "user_profile_not_found");
            }

            UserProfile userDetails = userProfileOpt.get();
            var userId = userDetails.getUserId();

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

            event.setOrganizerId(userId);
            populateFormattedTimes(event);
            var savedEvent = eventRepository.save(event);

            List<String> eventAdminIds = new ArrayList<>(userDetails.getEventAdminIds());
            eventAdminIds.add(savedEvent.getId());
            userDetails.setEventAdminIds(eventAdminIds);
            userProfileRepository.save(userDetails);

            return ServiceResult.success(savedEvent);

        } catch (DataAccessException e) {
            logger.error("Failed to create event due to a database error", e);
            return ServiceResult.error("Failed to create event due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while creating event.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    public ServiceResult<List<Event>> findAll() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Event> events = eventRepository.findBySoonestStartTimeAfterOrderBySoonestStartTimeAsc(now);
            return ServiceResult.success(events);
        } catch (DataAccessException e) {
            logger.error("Failed to retrieve events due to a database error", e);
            return ServiceResult.error("Failed to retrieve events due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while retrieving events.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }


    public ServiceResult<Event> findEventById(String id) {
        try {
            Optional<Event> eventOpt = eventRepository.findById(id);
            if (eventOpt.isEmpty()) {
                logger.warn("Event not found with id: {}", id);
                return ServiceResult.error("Event not found.", "event_not_found");
            }
            return ServiceResult.success(eventOpt.get());
        } catch (DataAccessException e) {
            logger.error("Failed to retrieve event due to a database error", e);
            return ServiceResult.error("Failed to retrieve event due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while retrieving event.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    public ServiceResult<List<Event>> findEventsByUser() {
        try {
            Optional<UserProfile> userProfileOpt = userService.getUserProfile();
            if (userProfileOpt.isEmpty()) {
                logger.error("Failed to retrieve events: could not retrieve user profile.");
                return ServiceResult.error("Failed to retrieve events: could not retrieve user profile.", "user_profile_not_found");
            }

            UserProfile userDetails = userProfileOpt.get();
            List<Event> events = eventRepository.findAllById(userDetails.getEventAdminIds());
            return ServiceResult.success(events);
        } catch (DataAccessException e) {
            logger.error("Failed to retrieve events due to a database error", e);
            return ServiceResult.error("Failed to retrieve events due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while retrieving events.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    public ServiceResult<Void> updateEvent(String id, Event updatedEvent) {
        try {
            Event existingEvent = eventRepository.findById(id).orElseThrow(() ->
                    new IllegalArgumentException("Event not found")
            );

            Optional<UserProfile> userProfileOpt = userService.getUserProfile();
            if (userProfileOpt.isEmpty()) {
                logger.error("Failed to update event: could not retrieve user profile.");
                return ServiceResult.error("Failed to update event: could not retrieve user profile.", "user_profile_not_found");
            }

            UserProfile userDetails = userProfileOpt.get();
            if (!userDetails.getEventAdminIds().contains(id)) {
                logger.warn("User doesn't have permission to update the event with id: {}", id);
                return ServiceResult.error("User doesn't have permission to update this event.", "permission_denied");
            }

            existingEvent.setName(updatedEvent.getName());
            existingEvent.setDescription(updatedEvent.getDescription());
            existingEvent.setLocationName(updatedEvent.getLocationName());
            existingEvent.setAddress(updatedEvent.getAddress());
            existingEvent.setDays(updatedEvent.getDays());

            LocalDateTime soonestStartTime = updatedEvent.getDays().stream()
                    .map(DayEvent::getStartTime)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
            existingEvent.setSoonestStartTime(soonestStartTime);

            updatedEvent.getDays().forEach(dayEvent -> {
                if (dayEvent.getEndTime() != null && dayEvent.getEndTime().isBefore(dayEvent.getStartTime())) {
                    throw new IllegalArgumentException("End time(s) must be after the start time.");
                }
            });

            populateFormattedTimes(existingEvent);

            eventRepository.save(existingEvent);

            return ServiceResult.success();
        } catch (DataAccessException e) {
            logger.error("Failed to update event due to a database error", e);
            return ServiceResult.error("Failed to update event due to a database error. Please try again later.", "database_error");
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument while updating event: {}", e.getMessage());
            return ServiceResult.error(e.getMessage(), "invalid_argument");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while updating event.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    public ServiceResult<Void> deleteEvent(String eventId) {
        try {
            Optional<UserProfile> userProfileOpt = userService.getUserProfile();
            if (userProfileOpt.isEmpty()) {
                logger.error("Failed to delete event: could not retrieve user profile.");
                return ServiceResult.error("Failed to delete event: could not retrieve user profile.", "user_profile_not_found");
            }

            UserProfile userDetails = userProfileOpt.get();
            if (!userDetails.getEventAdminIds().contains(eventId)) {
                logger.warn("User doesn't have permission to delete the event with id: {}", eventId);
                return ServiceResult.error("User doesn't have permission to delete this event.", "permission_denied");
            }

            eventRepository.deleteById(eventId);
            logger.info("Event with id {} deleted successfully", eventId);
            return ServiceResult.success();

        } catch (DataAccessException e) {
            logger.error("Failed to delete event due to a database error", e);
            return ServiceResult.error("Failed to delete event due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while deleting event.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    public ServiceResult<Void> cancelEvent(String eventId) {
        try {
            ServiceResult<Event> eventResult = findEventById(eventId);
            if (!eventResult.isSuccess()) {
                return ServiceResult.error("Could not cancel event due to a database error.", eventResult.getErrorCode());
            }

            Event event = eventResult.getData();
            event.setStatus("cancelled");
            eventRepository.save(event);
            logger.info("Event with id {} was successfully cancelled", eventId);

            return ServiceResult.success();

        } catch (DataAccessException e) {
            logger.error("Failed to cancel event due to a database error", e);
            return ServiceResult.error("Failed to cancel event due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while cancelling event.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    public ServiceResult<Void> reactivateEvent(String eventId) {
        try {
            ServiceResult<Event> eventResult = findEventById(eventId);
            if (!eventResult.isSuccess()) {
                return ServiceResult.error("Could not reactivate event due to a database error.", eventResult.getErrorCode());
            }

            Event event = eventResult.getData();
            event.setStatus("active");
            eventRepository.save(event);
            logger.info("Event with id {} was successfully reactivated", eventId);

            return ServiceResult.success();

        } catch (DataAccessException e) {
            logger.error("Failed to reactivate event due to a database error", e);
            return ServiceResult.error("Failed to reactivate event due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while reactivating event.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
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
