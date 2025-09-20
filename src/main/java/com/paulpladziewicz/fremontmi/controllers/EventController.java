package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.app.config.HtmlSanitizationService;
import com.paulpladziewicz.fremontmi.app.exceptions.UserNotAuthenticatedException;
import com.paulpladziewicz.fremontmi.content.Content;
import com.paulpladziewicz.fremontmi.content.ContentService;
import com.paulpladziewicz.fremontmi.content.ContentStatus;
import com.paulpladziewicz.fremontmi.content.ContentType;
import com.paulpladziewicz.fremontmi.content.DayEvent;
import com.paulpladziewicz.fremontmi.content.Event;
import com.paulpladziewicz.fremontmi.content.EventDto;
import com.paulpladziewicz.fremontmi.content.InteractionService;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.user.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class EventController {

    private final HtmlSanitizationService htmlSanitizationService;

    private final ContentService contentService;

    private final UserService userService;

    private final InteractionService interactionService;

    public EventController(HtmlSanitizationService htmlSanitizationService, ContentService contentService, UserService userService, InteractionService interactionService) {
        this.htmlSanitizationService = htmlSanitizationService;
        this.contentService = contentService;
        this.userService = userService;
        this.interactionService = interactionService;
    }

    @GetMapping("/create/event")
    public String displayCreateForm(Model model) {
        Event event = new Event();

        if (event.getDays() == null || event.getDays().isEmpty()) {
            List<DayEvent> days = new ArrayList<>();
            days.add(new DayEvent());
            event.setDays(days);
        }

        model.addAttribute("event", event);

        return "events/create-event";
    }

    @GetMapping("/admin/create/event")
    public String displayAdminForm(Model model) {
        Event event = new Event();

        if (event.getDays() == null || event.getDays().isEmpty()) {
            List<DayEvent> days = new ArrayList<>();
            days.add(new DayEvent());
            event.setDays(days);
        }

        model.addAttribute("event", event);

        return "events/admin-create-event";
    }

    @PostMapping("/create/event")
    public String createEvent(@Valid @ModelAttribute("event") EventDto eventDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", eventDto.getTags()));
            return "events/create-event";
        }

        if (eventDto.getDays() == null || eventDto.getDays().isEmpty()) {
            result.rejectValue("days", "error.event", "Please provide at least one date and time for the event.");
            model.addAttribute("tagsAsString", String.join(",", eventDto.getTags()));
            return "events/create-event";
        }

        try {
            Content savedEvent = contentService.create(ContentType.EVENT, eventDto);
            return "redirect:" + savedEvent.getPathname();
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();

            int index = extractIndexFromMessage(message);
            if (index >= 0) {
                result.rejectValue("days[" + index + "].endTime", "error.event", "End time must be after start time.");
            } else {
                result.rejectValue("days", "error.event", message);
            }

            model.addAttribute("tagsAsString", String.join(",", eventDto.getTags()));

            return "events/create-event";
        }
    }

    @GetMapping("/events")
    public String displayEvents(Model model) {
        return "spa";
    }

    @GetMapping("/api/events")
    public ResponseEntity<Page<Content>> getEvents(@RequestParam(defaultValue = "0") int page) {
        Page<Content> events = contentService.findEvents(page);

        LocalDateTime now = LocalDateTime.now();

        events.getContent().forEach(obj -> {
            if (obj.getDetail() instanceof Event event) {
                List<DayEvent> futureDayEvents = event.getDays().stream()
                        .filter(dayEvent -> dayEvent.getStartTime().isAfter(now))
                        .toList();

                if (!futureDayEvents.isEmpty()) {
                    event.setNextAvailableDayEvent(futureDayEvents.get(0));
                    event.setAvailableDayEventCount(futureDayEvents.size() - 1);
                } else {
                    event.setNextAvailableDayEvent(null);
                    event.setAvailableDayEventCount(0);
                }
            }
        });

        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/page/{currentPage}")
    public String displayNextEvents(@PathVariable int currentPage, Model model) {
        Page<Content> events = contentService.findEvents(currentPage + 1);

        LocalDateTime now = LocalDateTime.now();

        events.getContent().forEach(obj -> {
            if (obj.getDetail() instanceof Event event) {
                List<DayEvent> futureDayEvents = event.getDays().stream()
                        .filter(dayEvent -> dayEvent.getStartTime().isAfter(now))
                        .toList();

                if (!futureDayEvents.isEmpty()) {
                    event.setNextAvailableDayEvent(futureDayEvents.getFirst());
                    event.setAvailableDayEventCount(futureDayEvents.size() - 1);
                } else {
                    event.setNextAvailableDayEvent(null);
                    event.setAvailableDayEventCount(0);
                }
            }
        });

        model.addAttribute("events", events);
        return "events/partials/list-events";
    }

    @GetMapping("/my/events")
    public String displayMyEvents(Model model) {
        List<Content> events = contentService.findByUserAndType(ContentType.EVENT);

        model.addAttribute("events", events);

        return "events/my-events";
    }

    @GetMapping("/event/{slug}")
    public String displayEvent(@PathVariable String slug, @RequestParam(required = false, defaultValue = "false") boolean partial, Model model) {
        Content content = contentService.findByPathname('/' + ContentType.EVENT.toHyphenatedString() + '/' + slug, ContentType.EVENT);
        Event detail = (Event) content.getDetail();

        detail.setDescription(htmlSanitizationService.sanitizeHtml(detail.getDescription().replace("\n", "<br/>")));

        if (content.getStatus() == ContentStatus.CANCELLED) {
            model.addAttribute("canceled", "This event has been canceled.");
        }

        model.addAttribute("event", content);

        try {
            String userId = userService.getUserId();
            model.addAttribute("isAdmin", content.getCreatedBy().equals(userId));
        } catch (UserNotAuthenticatedException e) {
            model.addAttribute("isAdmin", false);
        }

        model.addAttribute("partial", partial);

        return partial ? "events/partials/event-partial" : "events/event-page";
    }

    @GetMapping("/edit/event/{slug}")
    public String displayEditForm(@PathVariable String slug, Model model) {
        Content content = contentService.findByPathname('/' + ContentType.EVENT.toHyphenatedString() + '/' + slug, ContentType.EVENT);
        EventDto event = createDto(content);

        String tagsAsString = String.join(",", event.getTags());
        model.addAttribute("tagsAsString", tagsAsString);

        model.addAttribute("event", event);

        return "events/edit-event";
    }

    @PostMapping("/edit/event")
    public String updateEvent(@ModelAttribute("event") @Valid EventDto eventDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", eventDto.getTags()));
            return "events/edit-event";
        }

        Content savedEvent = contentService.update(eventDto);

        return "redirect:" + savedEvent.getPathname();
    }

    @PostMapping("/delete/event")
    public String deleteGroup(@NotNull @RequestParam("contentId") String contentId) {
        contentService.delete(contentId);

        return "redirect:/events";
    }

    @PostMapping("/create/event/add-day")
    public String addDayToCreateForm(@ModelAttribute("event") Event event, Model model) {
        event.getDays().add(new DayEvent());
        model.addAttribute("event", event);
        return "events/htmx/adjust-day-events";
    }

    @PostMapping("/create/event/remove-day")
    public String removeDayToCreateForm(@ModelAttribute("event") Event event, Model model) {
        if (!event.getDays().isEmpty()) {
            event.getDays().removeLast();
        }
        model.addAttribute("event", event);
        return "events/htmx/adjust-day-events";
    }

    @PostMapping("/cancel/event")
    public String cancelEvent(@NotNull @RequestParam("slug") String slug) {
        interactionService.cancel(slug);

        return "redirect:/events/" + slug;
    }

    @PostMapping("/reactivate/event")
    public String reactivateEvent(@NotNull @RequestParam("slug") String slug) {
        interactionService.reactivate(slug);

        return "redirect:/events/" + slug;
    }

    private EventDto createDto(Content content) {
        if (!(content.getDetail() instanceof Event eventDetail)) {
            throw new IllegalArgumentException("ContentDto is not a EventDto");
        }

        EventDto dto = new EventDto();
        dto.setContentId(content.getId());
        dto.setStatus(content.getStatus());
        dto.setPathname(content.getPathname());
        dto.setTags(content.getTags());
        dto.setTitle(eventDetail.getTitle());
        dto.setDescription(eventDetail.getDescription());
        dto.setLocationName(eventDetail.getLocationName());
        dto.setAddress(eventDetail.getAddress());
        dto.setExternalUrl(eventDetail.getExternalUrl());
        dto.setDays(eventDetail.getDays());

        return dto;
    }


    private int extractIndexFromMessage(String message) {
        try {
            return Integer.parseInt(message.replaceAll("[^0-9]", "")) - 1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}