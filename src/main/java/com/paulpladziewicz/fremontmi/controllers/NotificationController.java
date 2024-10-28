package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Announcement;
import com.paulpladziewicz.fremontmi.models.ContactFormRequest;
import com.paulpladziewicz.fremontmi.models.EmailGroupRequest;
import com.paulpladziewicz.fremontmi.models.Group;
import com.paulpladziewicz.fremontmi.services.EmailService;
import com.paulpladziewicz.fremontmi.services.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class NotificationController {

    NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

//    @PostMapping("/article-contact")
//    public ResponseEntity<String> submitContactForm(@RequestBody @Valid ContactFormRequest request) {
//        try {
//            emailService.sendContactUsEmailAsync(request.getName(), request.getEmail(), "Parks article submission: " + request.getMessage());
//
//            return ResponseEntity.ok("Form submitted successfully");
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to submit the form");
//        }
//    }
//
//    @PostMapping("/taqueria")
//    public ResponseEntity<String> taqueriaContactForm(@RequestBody @Valid ContactFormRequest contactFormRequest) {
//        try {
//            emailService.generalContactForm("ppladziewicz@gmail.com",
//                    contactFormRequest.getName(),
//                    contactFormRequest.getEmail(),
//                    contactFormRequest.getMessage());
//
//            return ResponseEntity.ok("Form submitted successfully");
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to submit the form");
//        }
//    }
//
//
//
//
//    @PostMapping("/contact/business")
//    public ResponseEntity<Map<String, Object>> handleContactForm(
//            @RequestBody ContactFormRequest contactFormRequest) {
//
//        Boolean contactFormSuccess = contentService.handleContactFormSubmission(
//                contactFormRequest.getSlug(),
//                contactFormRequest.getName(),
//                contactFormRequest.getEmail(),
//                contactFormRequest.getMessage()
//        );
//
//        Map<String, Object> response = new HashMap<>();
//
//        if (!contactFormSuccess) {
//            response.put("success", false);
//            response.put("message", "An error occurred while trying to send your message. Please try again later.");
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//
//        response.put("success", true);
//        response.put("message", "We've passed your message along! We hope you hear back soon.");
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping("/contact/neighbor-services-profile")
//    public ResponseEntity<String> handleContactForm(@RequestBody @Valid ContactFormRequest contactFormRequest) {
//        try {
//            contentService.handleContactFormSubmission(
//                    contactFormRequest.getId(),
//                    contactFormRequest.getName(),
//                    contactFormRequest.getEmail(),
//                    contactFormRequest.getMessage()
//            );
//
//            return ResponseEntity.ok("Form submitted successfully");
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to submit the form");
//        }
//    }

    @GetMapping("/create/announcement")
    public String getGroupAnnouncementForm(@NotNull @PathVariable String groupId, Model model) {
        model.addAttribute("groupId", groupId);
        model.addAttribute("announcement", new Announcement());
        return "groups/htmx/group-announcements-form";
    }

    @PostMapping("/create/announcement")
    public String addGroupAnnouncement(@NotNull @PathVariable String contentId, @Valid Announcement announcement, Model model) {
        announcement.setCreationDate(Instant.now());

        Announcement createdAnnouncement = notificationService.createAnnouncement(contentId, announcement);

        model.addAttribute("announcement", createdAnnouncement);
        return "groups/htmx/group-announcements";
    }

    @PostMapping("/delete/announcement")
    public String deleteGroupAnnouncement(@NotNull @RequestParam("contentId") String contentId, @NotNull @RequestParam("announcementId") String announcementId) {
        notificationService.deleteAnnouncement(contentId, Integer.parseInt(announcementId));

        return "";
    }
}
