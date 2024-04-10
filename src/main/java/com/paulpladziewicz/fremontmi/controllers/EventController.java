package com.paulpladziewicz.fremontmi.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EventController {

    @GetMapping("/events")
    public String getAllEvents(Model model) {
        return "events";
    }
}