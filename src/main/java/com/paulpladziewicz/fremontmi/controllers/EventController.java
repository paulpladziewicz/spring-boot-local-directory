package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.EventService;
import com.paulpladziewicz.fremontmi.services.HtmlSanitizationService;
import com.paulpladziewicz.fremontmi.services.TagService;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class EventController {

    private final HtmlSanitizationService htmlSanitizationService;

    private final EventService eventService;

    private final UserService userService;

    private final TagService tagService;

    public EventController(HtmlSanitizationService htmlSanitizationService, EventService eventService, UserService userService, TagService tagService) {
        this.htmlSanitizationService = htmlSanitizationService;
        this.eventService = eventService;
        this.userService = userService;
        this.tagService = tagService;
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
    public String createEvent(@Valid @ModelAttribute("event") Event event, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "events/create-event";
        }

        ServiceResponse<Event> serviceResponse = eventService.createEvent(event);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "events/create-event";
        }

        Event savedEvent = serviceResponse.value();

        return "redirect:/events/" + savedEvent.getSlug();
    }

    @GetMapping("/events")
    public String displayEvents(@RequestParam(value = "tag", required = false) String tag, Model model) {
        ServiceResponse<List<Event>> serviceResponse = eventService.findAll(tag);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "events/events";
        }

        List<Event> events = serviceResponse.value();

        LocalDateTime now = LocalDateTime.now();

        // Process each event to find the next available day event and count future days
        events.forEach(event -> {
            // Filter future DayEvents
            List<DayEvent> futureDayEvents = event.getDays().stream()
                    .filter(dayEvent -> dayEvent.getStartTime().isAfter(now))
                    .collect(Collectors.toList());

            // Check if there's any future DayEvent
            if (!futureDayEvents.isEmpty()) {
                // Set the next available DayEvent as the first future day
                event.setNextAvailableDayEvent(futureDayEvents.get(0));
                event.setMoreDayEventsCount(futureDayEvents.size() - 1); // count all future days except the next one
            } else {
                event.setNextAvailableDayEvent(null);
                event.setMoreDayEventsCount(0);
            }
        });

        List<Content> contentList = new ArrayList<>(events);
        List<TagUsage> popularTags = tagService.getTagUsageFromContent(contentList, 15);
        model.addAttribute("popularTags", popularTags);
        model.addAttribute("selectedTag", tag);

        model.addAttribute("events", events);

        return "events/events";
    }

    @GetMapping("/events/{slug}")
    public String displayEvent(@PathVariable String slug, Model model) {
        ServiceResponse<Event> serviceResponse = eventService.findEventBySlug(slug);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "events/events";
        }

        Event event = serviceResponse.value();

        event.setDescription(htmlSanitizationService.sanitizeHtml(event.getDescription().replace("\n", "<br/>")));

        Optional<String> userIdOpt = userService.getUserId();

        if (userIdOpt.isEmpty()) {
            model.addAttribute("error", true);
            return "events/events";
        }

        String userId = userIdOpt.get();

        if ("canceled".equals(event.getStatus())) {
            model.addAttribute("canceled", "This event has been canceled.");
        }

        model.addAttribute("event", event);
        model.addAttribute("isAdmin", event.getCreatedBy().equals(userId));

        return "events/event-page";
    }

    @GetMapping("/edit/event/{slug}")
    public String displayEditForm(@PathVariable String slug, Model model) {
        ServiceResponse<Event> serviceResponse = eventService.findEventBySlug(slug);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "events/events";
        }

        Event event = serviceResponse.value();

        String tagsAsString = String.join(",", event.getTags());
        model.addAttribute("tagsAsString", tagsAsString);

        model.addAttribute("event", event);

        return "events/edit-event";
    }

    @PostMapping("/edit/event")
    public String updateEvent(@Valid @ModelAttribute("event") Event event, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "events/edit-event";
        }

        ServiceResponse<Event> serviceResponse = eventService.updateEvent(event);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "events/edit-event";
        }

        Event savedEvent = serviceResponse.value();

        return "redirect:/events/" + savedEvent.getSlug();
    }

    @GetMapping("/my/events")
    public String displayMyEvents(Model model) {
        ServiceResponse<List<Event>> serviceResponse = eventService.findEventsByUser();

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "events/events";
        }

        List<Event> events = serviceResponse.value();

        model.addAttribute("events", events);

        return "events/my-events";
    }

    @PostMapping("/create/event/add-day")
    public String addDayToCreateForm(@ModelAttribute("event") Event event, Model model) {
        event.getDays().add(new DayEvent());
        model.addAttribute("event", event);
        return "events/htmx/adjust-day-events"; // Ensure this fragment only contains the "dayEvents" div content
    }

    @PostMapping("/create/event/remove-day")
    public String removeDayToCreateForm(@ModelAttribute("event") Event event, Model model) {
        if (!event.getDays().isEmpty()) {
            event.getDays().removeLast(); // Adjusted to avoid potential empty list exception
        }
        model.addAttribute("event", event);
        return "events/htmx/adjust-day-events"; // Ensure this fragment only contains the "dayEvents" div content
    }

    @PostMapping("/cancel/event")
    public String cancelEvent(@NotNull @RequestParam("slug") String slug, RedirectAttributes redirectAttributes) {
        ServiceResponse<Boolean> serviceResponse = eventService.cancelEvent(slug);

        if (serviceResponse.hasError()) {
            redirectAttributes.addFlashAttribute("error", true);
        }

        return "redirect:/events/" + slug;
    }

    @PostMapping("/reactivate/event")
    public String uncancelEvent(@NotNull @RequestParam("slug") String slug, RedirectAttributes redirectAttributes) {
        ServiceResponse<Boolean> serviceResponse = eventService.reactivateEvent(slug);

        if (serviceResponse.hasError()) {
            redirectAttributes.addFlashAttribute("error", true);
        }

        return "redirect:/events/" + slug;
    }

    @PostMapping("/delete/event")
    public String deleteGroup(@NotNull @RequestParam("eventId") String eventId, RedirectAttributes redirectAttributes) {
        ServiceResponse<Boolean> serviceResponse = eventService.deleteEvent(eventId);

        if (serviceResponse.hasError()) {
            redirectAttributes.addFlashAttribute("error", true);
        }

        return "redirect:/events";
    }
}