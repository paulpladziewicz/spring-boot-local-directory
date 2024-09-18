package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.DayEvent;
import com.paulpladziewicz.fremontmi.models.Event;
import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.services.EventService;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class EventController {

    private final EventService eventService;
    private final UserService userService;

    public EventController(EventService eventService, UserService userService) {
        this.eventService = eventService;
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

        return "redirect:/events/" + savedEvent.getId();
    }

    @GetMapping("/events")
    public String displayGroups(Model model) {
        ServiceResponse<List<Event>> serviceResponse = eventService.findAll();

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "events/events";
        }

        List<Event> events = serviceResponse.value();

        model.addAttribute("events", events);

        return "events/events";
    }

    @GetMapping("/events/{id}")
    public String displayEvent(@PathVariable String id, Model model) {
        ServiceResponse<Event> serviceResponse = eventService.findEventById(id);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "events/events";
        }

        Event event = serviceResponse.value();

        Optional<String> userIdOpt = userService.getUserId();

        if (userIdOpt.isEmpty()) {
            model.addAttribute("error", true);
            return "events/events";
        }

        String userId = userIdOpt.get();

        if ("cancelled".equals(event.getStatus())) {
            model.addAttribute("cancelled", "This event has been cancelled.");
        }

        model.addAttribute("event", event);
        model.addAttribute("isAdmin", event.getCreatedBy().equals(userId));

        return "events/event-page";
    }

    @GetMapping("/edit/event/{id}")
    public String displayEditForm(@PathVariable String id, Model model) {
        ServiceResponse<Event> serviceResponse = eventService.findEventById(id);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "events/events";
        }

        Event event = serviceResponse.value();

        model.addAttribute("event", event);

        return "events/edit-event";
    }

    @PostMapping("/edit/event/{id}")
    public String updateEvent(@PathVariable String id, @Valid @ModelAttribute("event") Event event, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "events/edit-event";
        }

        ServiceResponse<Event> serviceResponse = eventService.updateEvent(id, event);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "events/edit-event";
        }

        return "redirect:/events/" + id;
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

    @PostMapping("/cancel/event")
    public String cancelEvent(@NotNull @RequestParam("eventId") String eventId, RedirectAttributes redirectAttributes) {
        ServiceResponse<Boolean> serviceResponse = eventService.cancelEvent(eventId);

        if (serviceResponse.hasError()) {
            redirectAttributes.addFlashAttribute("error", true);
        }

        return "redirect:/events/" + eventId;
    }

    @PostMapping("/reactivate/event")
    public String uncancelEvent(@NotNull @RequestParam("eventId") String eventId, RedirectAttributes redirectAttributes) {
        ServiceResponse<Boolean> serviceResponse = eventService.reactivateEvent(eventId);

        if (serviceResponse.hasError()) {
            redirectAttributes.addFlashAttribute("error", true);
        }

        return "redirect:/events/" + eventId;
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