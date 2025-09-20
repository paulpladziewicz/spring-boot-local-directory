package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.notification.AnnouncementDto;
import com.paulpladziewicz.fremontmi.notification.NotificationService;
import com.paulpladziewicz.fremontmi.notification.SimpleContactFormSubmission;
import com.paulpladziewicz.fremontmi.notification.Subscriber;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class NotificationController {

    NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/subscribe")
    public String handleSubscribeForm(@RequestParam("email") @Email String email, RedirectAttributes redirectAttributes) {

        notificationService.subscribe(email);

        redirectAttributes.addFlashAttribute("subscribedMessage", "Thank you for subscribing to updates.");

        return "redirect:/#subscribe";
    }

    @PostMapping("/api/subscribe")
    @ResponseBody
    public ResponseEntity<String> handleSubscribeForm(@RequestBody @Valid Subscriber subscriber) {
        notificationService.subscribe(subscriber);
        return ResponseEntity.ok("Thank you for subscribing to updates");
    }

    @PostMapping("/contact")
    @ResponseBody
    public ResponseEntity<String> handleContactForm(@RequestBody @Valid SimpleContactFormSubmission submission) {
        try {
            notificationService.handleContactFormSubmission(submission);
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }

    }

    @PostMapping("/create/announcement")
    @ResponseBody
    public ResponseEntity<String> addGroupAnnouncement(@RequestBody @Valid AnnouncementDto announcementDto) {
        try {
            notificationService.createAnnouncement(announcementDto);
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create announcement");
        }
    }

    @PostMapping("/delete/announcement")
    @ResponseBody
    public ResponseEntity<String> deleteGroupAnnouncement(@RequestBody AnnouncementDto announcementDto) {
        notificationService.deleteAnnouncement(announcementDto);
        return ResponseEntity.ok("Success");
    }
}
