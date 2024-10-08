package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.exceptions.ContentNotFoundException;
import com.paulpladziewicz.fremontmi.exceptions.PermissionDeniedException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
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
        UserProfile userProfile = userService.getUserProfile();
        String userId = userProfile.getUserId();

        validateEventTimes(event);

        event.setCreatedBy(userId);
        populateFormattedTimes(event);
        event.setType(ContentTypes.EVENT.getContentType());
        event.setSlug(createUniqueSlug(event.getName()));
        event.setPathname("/events/" + event.getSlug());

        List<String> validatedTags = tagService.addTags(event.getTags(), ContentTypes.EVENT.getContentType());
        event.setTags(validatedTags);

        Event savedEvent = saveEvent(event);

        userProfile.getEventIds().add(savedEvent.getId());
        userService.saveUserProfile(userProfile);

        return ServiceResponse.value(savedEvent);
    }

    public Event saveEvent(Event event) {
        return contentRepository.save(event);
    }

    public String createUniqueSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String baseSlug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
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

    public List<Event> findAll(String tag) {
        LocalDateTime now = LocalDateTime.now();
        Sort sort = Sort.by("days.startTime").ascending();

        if (tag != null && !tag.trim().isEmpty()) {
            return contentRepository.findByTagAndAnyFutureDayEventOrderBySoonestStartTimeAsc(tag, now, sort);
        } else {
            return contentRepository.findByAnyFutureDayEventOrderBySoonestStartTimeAsc(now, sort);
        }
    }

    public Optional<Event> findEventById(String id) {
        return contentRepository.findById(id, Event.class);
    }

    public Optional<Event> findEventBySlug(String slug) {
        return contentRepository.findBySlugAndType(slug, ContentTypes.EVENT.getContentType(), Event.class);
    }

    public List<Event> findEventsByUser() {
        UserProfile userProfile = userService.getUserProfile();

        return contentRepository.findAllById(userProfile.getEventIds(), Event.class);
    }

    @Transactional
    public Event updateEvent(Event updatedEvent) {
        UserProfile userProfile = userService.getUserProfile();

        Optional<Event> optionalEvent = findEventBySlug(updatedEvent.getSlug());

        if (optionalEvent.isEmpty()) {
            throw new ContentNotFoundException("Event with slug '" + updatedEvent.getSlug() + "' not found.");
        }

        Event existingEvent = optionalEvent.get();

        if (!hasPermission(userProfile.getUserId(), existingEvent)) {
            throw new PermissionDeniedException("User does not have permission to update this event.");
        }

        validateEventTimes(updatedEvent);

        List<String> oldTags = existingEvent.getTags();
        List<String> newTags = updatedEvent.getTags();

        if (newTags != null) {
            tagService.updateTags(newTags, oldTags != null ? oldTags : new ArrayList<>(), ContentTypes.EVENT.getContentType());
        }

        if (!existingEvent.getName().equals(updatedEvent.getName())) {
            String newSlug = createUniqueSlug(updatedEvent.getName());
            existingEvent.setSlug(newSlug);
            existingEvent.setPathname("/events/" + newSlug);
        }

        updateExistingEvent(existingEvent, updatedEvent);

        return saveEvent(existingEvent);

    }

    @Transactional
    public void deleteEvent(String eventId) {
        UserProfile userProfile = userService.getUserProfile();

        Optional<Event> optionalEvent = findEventById(eventId);

        if (optionalEvent.isEmpty()) {
            throw new ContentNotFoundException("Could not find event with id '" + eventId + "'.");

        }

        Event existingEvent = optionalEvent.get();

        if (!hasPermission(userProfile.getUserId(), existingEvent)) {
            throw new PermissionDeniedException("User does not have permission to delete this event.");
        }

        tagService.removeTags(existingEvent.getTags(), ContentTypes.EVENT.getContentType());

        List<String> eventIds = userProfile.getEventIds();
        eventIds.remove(existingEvent.getId());
        userProfile.setEventIds(eventIds);
        userService.saveUserProfile(userProfile);

        contentRepository.deleteById(eventId);
    }

    private Boolean hasPermission(String userId, Event event) {
        return event.getCreatedBy().equals(userId);
    }

    public Event cancelEvent(String slug) {
        return updateEventStatus(slug, "canceled");
    }

    public Event reactivateEvent(String slug) {
        return updateEventStatus(slug, "active");
    }

    private Event updateEventStatus(String slug, String status) {
        UserProfile userProfile = userService.getUserProfile();

        Optional<Event> optionalEvent = findEventBySlug(slug);

        if (optionalEvent.isEmpty()) {
            throw new ContentNotFoundException("Event with slug '" + slug + "' not found.");
        }

        Event existingEvent = optionalEvent.get();

        if (!hasPermission(userProfile.getUserId(), existingEvent)) {
            throw new PermissionDeniedException("User does not have permission to update this event.");
        }

        existingEvent.setStatus(status);

        return contentRepository.save(existingEvent);
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
        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}