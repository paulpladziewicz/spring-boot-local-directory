package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Event;
import com.paulpladziewicz.fremontmi.services.EventService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/events")
    public String displayGroups(Model model) {
        model.addAttribute("events", eventService.findAll());
        return "events/events";
    }

    @GetMapping("/events/{id}")
    public String displayEvent(@PathVariable String id, Model model) {
        Event event = eventService.findEventById(id);
        model.addAttribute("event", event);
        model.addAttribute("isEventAdmin", true);
        return "events/event-page";
    }

    @GetMapping("/my/events")
    public String displayMyEvents(Model model) {
        model.addAttribute("event", new Event());
        return "events/my-events";
    }

    @GetMapping("/create/event")
    public String displayCreateForm(Model model) {
        model.addAttribute("event", new Event());
        return "events/create-event";
    }

    @PostMapping("/create/event")
    public String createEvent(@Valid @ModelAttribute Event event, Model model) {
        if (event.getDays() == null || event.getDays().isEmpty()) {
            // Handle the error, e.g., return an error message or re-display the form with a message
            throw new IllegalArgumentException("At least one event date must be provided.");
        }

        Event savedEvent = eventService.createEvent(event);

        return "redirect:/events" + savedEvent.getId();
    }
}