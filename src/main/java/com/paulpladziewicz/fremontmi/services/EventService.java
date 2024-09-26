package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final ContentRepository contentRepository;

    private final UserService userService;
    private final TagService tagService;

    public EventService(ContentRepository contentRepository, UserService userService, TagService tagService) {
        this.contentRepository = contentRepository;
        this.userService = userService;
        this.tagService = tagService;
    }

    @Transactional
    public ServiceResponse<Event> createEvent(Event event) {
        try {
            Optional<UserProfile> userProfileOpt = userService.getUserProfile();
            if (userProfileOpt.isEmpty()) {
                return logAndReturnError("Failed to create event: user profile not found.", "user_profile_not_found");
            }

            UserProfile userProfile = userProfileOpt.get();
            String userId = userProfile.getUserId();

            validateEventTimes(event);

            event.setCreatedBy(userId);
            populateFormattedTimes(event);
            event.setType(ContentTypes.EVENT.getContentType());
            event.setSlug(createUniqueSlug(event.getName()));
            event.setPathname("/events/" + event.getSlug());

            ServiceResponse<Event> saveEventResponse = saveEvent(event);

            if (saveEventResponse.hasError()) {
                return saveEventResponse;
            }

            Event savedEvent = saveEventResponse.value();

            userProfile.getEventIds().add(savedEvent.getId());
            userService.saveUserProfile(userProfile);

            return ServiceResponse.value(savedEvent);
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while creating event.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while creating event.", "unexpected_error", e);
        }
    }

    public ServiceResponse<Event> saveEvent(Event event) {
        try {
            return ServiceResponse.value(contentRepository.save(event));
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to save event", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to save event", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public String createUniqueSlug(String name) {
        String baseSlug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");

        List<Content> matchingSlugs = contentRepository.findBySlugRegexAndType("^" + baseSlug + "(-\\d+)?$", ContentTypes.EVENT.getContentType());

        if (matchingSlugs.isEmpty()) {
            return baseSlug;
        }

        Pattern pattern = Pattern.compile(Pattern.quote(baseSlug) + "-(\\d+)$");

        int maxNumber = 0;
        boolean baseSlugExists = false;

        for (Content content : matchingSlugs) {
            String slug = content.getSlug();

            if (slug.equals(baseSlug)) {
                baseSlugExists = true;
            }

            Matcher matcher = pattern.matcher(slug);
            if (matcher.find()) {
                int number = Integer.parseInt(matcher.group(1));
                maxNumber = Math.max(maxNumber, number);
            }
        }

        if (baseSlugExists) {
            return baseSlug + "-" + (maxNumber + 1);
        } else if (maxNumber > 0) {
            return baseSlug + "-" + (maxNumber + 1);
        } else {
            return baseSlug;
        }
    }

    public ServiceResponse<List<Event>> findAll(String tag) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Event> events;

            if (tag != null && !tag.trim().isEmpty()) {
                // Fetch events filtered by the tag and having future day events
                events = contentRepository.findByTagAndAnyFutureDayEvent(tag, now);
                logger.info("Fetched {} events with tag '{}'", events.size(), tag);
            } else {
                // Fetch all events with future day events without tag filtering
                events = contentRepository.findByAnyFutureDayEvent(now);
                logger.info("Fetched {} events without any tag filtering", events.size());
            }

            return ServiceResponse.value(events);
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while retrieving events.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while retrieving events.", "unexpected_error", e);
        }
    }

    public ServiceResponse<Event> findEventById(String id) {
        try {
            return contentRepository.findById(id)
                    .filter(content -> content instanceof Event)
                    .map(content -> (Event) content)
                    .map(ServiceResponse::value)
                    .orElseGet(() -> logAndReturnError("Event not found with id: " + id, "event_not_found"));
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while retrieving event.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while retrieving event.", "unexpected_error", e);
        }
    }

    public ServiceResponse<Event> findEventBySlug(String slug) {
        try {
            return contentRepository.findBySlugAndType(slug, ContentTypes.EVENT.getContentType())
                    .filter(content -> content instanceof Event)
                    .map(content -> (Event) content)
                    .map(ServiceResponse::value)
                    .orElseGet(() -> logAndReturnError("Event not found with slug: " + slug, "event_not_found"));
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while retrieving event.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while retrieving event.", "unexpected_error", e);
        }
    }

    public ServiceResponse<List<Event>> findEventsByUser() {
        try {
            Optional<UserProfile> optionalUserProfile = userService.getUserProfile();
            if (optionalUserProfile.isEmpty()) {
                return logAndReturnError("Failed to retrieve events: user profile not found.", "user_profile_not_found");
            }

            UserProfile userProfile = optionalUserProfile.get();

            List<Content> contents = contentRepository.findAllById(userProfile.getEventIds());

            List<Event> events = contents.stream()
                    .filter(content -> content instanceof Event)
                    .map(content -> (Event) content)
                    .collect(Collectors.toList());

            return ServiceResponse.value(events);
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while retrieving events.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while retrieving events.", "unexpected_error", e);
        }
    }

    @Transactional
    public ServiceResponse<Event> updateEvent(Event updatedEvent) {
        try {
            Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

            if (optionalUserProfile.isEmpty()) {
                return logAndReturnError("Failed to update event: user profile not found.", "user_profile_not_found");
            }

            UserProfile userProfile = optionalUserProfile.get();

            ServiceResponse<Event> existingEventResult = findEventBySlug(updatedEvent.getSlug());

            if (existingEventResult.hasError()) {
                return ServiceResponse.error(existingEventResult.errorCode());
            }

            Event existingEvent = existingEventResult.value();

            if (!hasPermission(userProfile.getUserId(), existingEvent)) {
                return logAndReturnError("User does not have permission to update event: " + existingEvent.getId(), "permission_denied");
            }

            validateEventTimes(updatedEvent);

            List<String> oldTags = existingEvent.getTags();
            List<String> newTags = updatedEvent.getTags();

            if (newTags != null) {
                tagService.updateTags(newTags, oldTags != null ? oldTags : new ArrayList<>(), ContentTypes.EVENT.getContentType());
            }

            updateExistingEvent(existingEvent, updatedEvent);

            ServiceResponse<Event> saveEventResponse = saveEvent(existingEvent);

            if (saveEventResponse.hasError()) {
                return saveEventResponse;
            }

            return ServiceResponse.value(saveEventResponse.value());
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while updating event.", "database_error", e);
        } catch (IllegalArgumentException e) {
            return logAndReturnError("Invalid argument while updating event: " + e.getMessage(), "invalid_argument");
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while updating event.", "unexpected_error", e);
        }
    }


    @Transactional
    public ServiceResponse<Boolean> deleteEvent(String eventId) {
        try {
            Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

            if (optionalUserProfile.isEmpty()) {
                return logAndReturnError("Failed to create business: user profile not found.", "user_profile_not_found");
            }

            UserProfile userProfile = optionalUserProfile.get();

            ServiceResponse<Event> existingEventResult = findEventById(eventId);

            if (existingEventResult.hasError()) {
                return ServiceResponse.error(existingEventResult.errorCode());
            }

            Event existingEvent = existingEventResult.value();

            if (!hasPermission(userProfile.getUserId(), existingEvent)) {
                return logAndReturnError("User does not have permission to update event: " + eventId, "permission_denied");
            }

            tagService.removeTags(existingEvent.getTags(), ContentTypes.EVENT.getContentType());

            List<String> eventIds = userProfile.getEventIds();
            eventIds.remove(existingEvent.getId());
            userProfile.setEventIds(eventIds);
            userService.saveUserProfile(userProfile);

            contentRepository.deleteById(eventId);

            return ServiceResponse.value(true);
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while deleting event.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while deleting event.", "unexpected_error", e);
        }
    }

    private Boolean hasPermission(String userId, Event event) {
        return event.getCreatedBy().equals(userId);
    }

    public ServiceResponse<Boolean> cancelEvent(String slug) {
        return updateEventStatus(slug, "canceled", "Failed to cancel event.");
    }

    public ServiceResponse<Boolean> reactivateEvent(String slug) {
        return updateEventStatus(slug, "active", "Failed to reactivate event.");
    }

    private ServiceResponse<Boolean> updateEventStatus(String slug, String status, String errorMessage) {
        try {
            Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

            if (optionalUserProfile.isEmpty()) {
                return logAndReturnError("Failed to create business: user profile not found.", "user_profile_not_found");
            }

            UserProfile userProfile = optionalUserProfile.get();

            ServiceResponse<Event> existingEventResult = findEventBySlug(slug);

            if (existingEventResult.hasError()) {
                return ServiceResponse.error(existingEventResult.errorCode());
            }

            Event existingEvent = existingEventResult.value();

            if (!hasPermission(userProfile.getUserId(), existingEvent)) {
                return logAndReturnError("User does not have permission to update event: " + slug, "permission_denied");
            }

            existingEvent.setStatus(status);
            contentRepository.save(existingEvent);

            return ServiceResponse.value(true);
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

    private void updateExistingEvent(Event existingEvent, Event updatedEvent) {
        existingEvent.setName(updatedEvent.getName());
        existingEvent.setDescription(updatedEvent.getDescription());
        existingEvent.setLocationName(updatedEvent.getLocationName());
        existingEvent.setAddress(updatedEvent.getAddress());
        existingEvent.setDays(updatedEvent.getDays());

        if (updatedEvent.getTags() != null && !updatedEvent.getTags().isEmpty()) {
            existingEvent.setTags(updatedEvent.getTags());
        }

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