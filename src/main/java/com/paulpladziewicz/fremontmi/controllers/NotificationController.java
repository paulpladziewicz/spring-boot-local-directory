package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Announcement;
import com.paulpladziewicz.fremontmi.models.SimpleContactFormSubmission;
import com.paulpladziewicz.fremontmi.services.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;

@RestController
public class NotificationController {

    NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/subscribe")
    public String handleSubscribeForm(
            @RequestParam("email") @Email String email,
            RedirectAttributes redirectAttributes) {

        notificationService.subscribe(email);

        redirectAttributes.addFlashAttribute("subscribedMessage", "Thank you for subscribing to updates.");

        return "redirect:/#subscribe";
    }

    @PostMapping("/contact")
    public ResponseEntity<String> handleContactForm(SimpleContactFormSubmission submission) {
        try {
            notificationService.handleContactFormSubmission(submission);
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }

    }

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
