package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.DayEvent;
import com.paulpladziewicz.fremontmi.models.Event;
import com.paulpladziewicz.fremontmi.services.EventService;
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

    @GetMapping("/edit/event/{id}")
    public String displayEditForm(@PathVariable String id, Model model) {
        Event event = eventService.findEventById(id);
        model.addAttribute("event", event);
        return "events/edit-event";
    }

    @PostMapping("/edit/event/{id}")
    public String updateEvent(@PathVariable String id, @ModelAttribute Event event) {
//        eventService.updateEvent(id, event);
        return "redirect:/events/" + id;
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

    @PostMapping("/create/event")
    public String createEvent(@Valid @ModelAttribute("event") Event event, BindingResult result, Model model) {
        event.getDays().forEach(dayEvent -> {
            System.out.println("Start Time: " + dayEvent.getStartTime());
            System.out.println("End Time: " + dayEvent.getEndTime());
        });

        if (result.hasErrors()) {
            return "events/create-event";
        }


        Event savedEvent = eventService.createEvent(event);

        return "redirect:/events/" + savedEvent.getId();
    }

    @PostMapping("/delete/event")
    public String deleteGroup(@NotNull @RequestParam("eventId") String eventId) {
        eventService.deleteEvent(eventId);
        return "redirect:/events";
    }
}