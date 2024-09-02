package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.DayEvent;
import com.paulpladziewicz.fremontmi.models.Event;
import com.paulpladziewicz.fremontmi.services.EventService;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class EventController {

    private final EventService eventService;
    private final UserService userService;

    public EventController(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }

    @GetMapping("/events")
    public String displayGroups(Model model) {
        model.addAttribute("events", eventService.findAll());
        return "events/events";
    }

    @GetMapping("/events/{id}")
    public String displayEvent(@PathVariable String id, Model model) {
        Event event = eventService.findEventById(id);
        String userId = userService.getUserId();

        if ("cancelled".equals(event.getStatus())) {
            model.addAttribute("cancelled", "This event has been cancelled.");
        }

        model.addAttribute("event", event);
        model.addAttribute("isAdmin", event.getOrganizerId().equals(userId));
        return "events/event-page";
    }

    @GetMapping("/edit/event/{id}")
    public String displayEditForm(@PathVariable String id, Model model) {
        Event event = eventService.findEventById(id);
        if (event == null) {
            // Handle case where the event is not found (optional)
            return "redirect:/events"; // Redirect to event listing or an error page
        }
        model.addAttribute("event", event);
        return "events/edit-event";
    }

    @PostMapping("/edit/event/{id}")
    public String updateEvent(@PathVariable String id, @Valid @ModelAttribute("event") Event event, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "events/edit-event"; // Return to the edit page if there are validation errors
        }

        try {
            eventService.updateEvent(id, event);
        } catch (IllegalArgumentException e) {
            // Handle any exceptions thrown by the service, e.g., invalid date-time inputs
            model.addAttribute("errorMessage", e.getMessage());
            return "events/edit-event";
        }

        return "redirect:/events/" + id; // Redirect to the event's detail page after a successful update
    }

    @GetMapping("/my/events")
    public String displayMyEvents(Model model) {
        try {
            List<Event> eventDetails = eventService.findEventsByUser();
            model.addAttribute("events", eventDetails);
            return "events/my-events";
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return "home";
        }
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

    @PostMapping("/create/event/add-day")
    public String addDayToCreateForm(@ModelAttribute("event") Event event, Model model) {
        event.getDays().add(new DayEvent());

        model.addAttribute("event", event);
        return "events/htmx/adjust-days";
    }

    @PostMapping("/create/event/remove-day")
    public String removeDayToCreateForm(@ModelAttribute("event") Event event, Model model) {
        if (event.getDays().isEmpty()) {
            model.addAttribute("event", event);
            return "events/htmx/adjust-days";
        }

        event.getDays().removeLast();
        model.addAttribute("event", event);
        return "events/htmx/adjust-days";
    }

    @PostMapping("/create/event")
    public String createEvent(@Valid @ModelAttribute("event") Event event, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "events/create-event";
        }

        try {
            Event savedEvent = eventService.createEvent(event);
            return "redirect:/events/" + savedEvent.getId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "events/create-event";
        }
    }

    @PostMapping("/cancel/event")
    public String cancelEvent(@NotNull @RequestParam("eventId") String eventId) {
        eventService.cancelEvent(eventId);
        return "redirect:/events/" + eventId;
    }

    @PostMapping("/reactivate/event")
    public String uncancelEvent(@NotNull @RequestParam("eventId") String eventId) {
        eventService.reactivateEvent(eventId);
        return "redirect:/events/" + eventId;
    }

    @PostMapping("/delete/event")
    public String deleteGroup(@NotNull @RequestParam("eventId") String eventId) {
        eventService.deleteEvent(eventId);
        return "redirect:/events";
    }
}