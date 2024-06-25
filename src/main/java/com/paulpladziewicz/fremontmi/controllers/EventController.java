package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Event;
import com.paulpladziewicz.fremontmi.services.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/events")
    public String displayGroups(Model model) {
        model.addAttribute("events", eventService.findAll());
        return "events";
    }

    @GetMapping("/my/events")
    public String displayMyEvents(Model model) {
        model.addAttribute("event", new Event());
        return "dashboard/event-create";
    }

    @GetMapping("/my/events/create")
    public String displayCreateForm(Model model) {
        model.addAttribute("event", new Event());
        return "dashboard/event-create";
    }

    @PostMapping("/my/events/create")
    public String createEvent(@Valid @ModelAttribute Event event, Model model) {
        eventService.createEvent(event);

        return "redirect:/my/events";
    }
}