package com.paulpladziewicz.fremontmi.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class EventController {

    @GetMapping("/events")
    public String getAllEvents(Model model) {
        return "events";
    }

    @GetMapping("/my-events")
    public String getUserEvents(Model model) {
        return "my-events";
    }

    @GetMapping("/create-event")
    public String createEvent(Model model) {
        return "events";
    }

    @GetMapping("/edit-event")
    public String editEvent(Model model) {
        return "events";
    }

    @PostMapping("/delete-event")
    public String deleteEvent(Model model) {
        return "events";
    }
}