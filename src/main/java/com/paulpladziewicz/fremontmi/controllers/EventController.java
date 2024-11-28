package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.exceptions.UserNotAuthenticatedException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.ContentService;
import com.paulpladziewicz.fremontmi.services.HtmlSanitizationService;
import com.paulpladziewicz.fremontmi.services.InteractionService;
import com.paulpladziewicz.fremontmi.services.UserService;
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

    public EventController(HtmlSanitizationService htmlSanitizationService, ContentService contentService, UserService userService) {
        this.htmlSanitizationService = htmlSanitizationService;
        this.contentService = contentService;
        this.userService = userService;
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
}