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

    @GetMapping("/api/events")
    @ResponseBody
    public List<Event> getAllEvents() {
        return eventService.findAll();
    }

    @PostMapping("/api/events")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public Event createGroup(@Valid @RequestBody Event event) {
        return eventService.createEvent(event);
    }
}