package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.exceptions.UserNotAuthenticatedException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.ContentService;
import com.paulpladziewicz.fremontmi.services.HtmlSanitizationService;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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

    @PostMapping("/create/event")
    public String createEvent(@Valid @ModelAttribute("event") EventDto eventDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", eventDto.getTags()));
            return "events/create-event";
        }

        if (eventDto.getDays() == null || eventDto.getDays().isEmpty()) {
            result.rejectValue("days", "error.event", "Please provide at least one date and time for the event.");
            return "events/create-event";
        }

        Content savedEvent = contentService.create(ContentType.GROUP, eventDto);

        return "redirect:" + savedEvent.getPathname();
    }

    @GetMapping("/events")
    public String displayEvents(@RequestParam(value = "tag", required = false) String tag, @RequestParam(defaultValue = "0") int page,  Model model) {
        Page<Content> events;
        if (tag != null && !tag.isEmpty()) {
            events = contentService.findByTagAndType(tag, ContentType.EVENT, page);

        } else {
            events = contentService.findByType(ContentType.EVENT, page);
        }

        // TODO correct logic for displaying events in the future or are not expired

        model.addAttribute("events", events);

        return "events/events";
    }

    @GetMapping("/events/{slug}")
    public String displayEvent(@PathVariable String slug, Model model) {
        Content content = contentService.findByPathname('/' + ContentType.EVENT.getContentType() + '/' + slug, ContentType.EVENT);
        EventDto event = createDto(content);


        event.setDescription(htmlSanitizationService.sanitizeHtml(event.getDescription().replace("\n", "<br/>")));

        if ("canceled".equals(content.getStatus())) {
            model.addAttribute("canceled", "This event has been canceled.");
        }

        model.addAttribute("event", event);

        try {
            String userId = userService.getUserId();
            model.addAttribute("isAdmin", content.getCreatedBy().equals(userId));
        } catch (UserNotAuthenticatedException e) {
            model.addAttribute("isAdmin", false);
        }

        return "events/event-page";
    }

    @GetMapping("/my/events")
    public String displayMyEvents(Model model) {
        List<Content> events = contentService.findByUserAndType(ContentType.EVENT);

        model.addAttribute("events", events);

        return "events/my-events";
    }

    @GetMapping("/edit/event/{slug}")
    public String displayEditForm(@RequestParam(value = "contentId") String contentId, Model model) {
        Content content = contentService.findById(contentId);
        EventDto event = createDto(content);

        String tagsAsString = String.join(",", event.getTags());
        model.addAttribute("tagsAsString", tagsAsString);

        model.addAttribute("event", event);

        return "events/edit-event";
    }

    @PostMapping("/edit/event")
    public String updateEvent(@NotNull @RequestParam("contentId") String contentId, @Valid @ModelAttribute("event") EventDto eventDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", eventDto.getTags()));
            return "events/edit-event";
        }

        Content savedEvent = contentService.update(contentId, eventDto);

        return "redirect:" + savedEvent.getPathname();
    }

    @PostMapping("/delete/event")
    public String deleteGroup(@NotNull @RequestParam("eventId") String eventId) {
        contentService.delete(eventId);

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

    private EventDto createDto(Content content) {
        if (!(content.getDetail() instanceof Event eventDetail)) {
            throw new IllegalArgumentException("ContentDto is not a EventDto");
        }

        EventDto dto = new EventDto();
        dto.setContentId(content.getId());
        dto.setPathname(content.getPathname());
        dto.setTitle(eventDetail.getTitle());
        dto.setDescription(eventDetail.getDescription());

        return dto;
    }
}