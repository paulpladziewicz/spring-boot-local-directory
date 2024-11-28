package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.exceptions.UserNotAuthenticatedException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.*;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class GroupController {

    private final HtmlSanitizationService htmlSanitizationService;

    private final ContentService contentService;
    private final NotificationService notificationService;

    InteractionService interactionService;

    private final UserService userService;


    public GroupController(HtmlSanitizationService htmlSanitizationService, ContentService contentService, UserService userService, InteractionService interactionService, NotificationService notificationService) {
        this.htmlSanitizationService = htmlSanitizationService;
        this.contentService = contentService;
        this.userService = userService;
        this.interactionService = interactionService;
        this.notificationService = notificationService;
    }

    @GetMapping("/groups")
    public String displayGroups() {
        return "spa";
    }

    @GetMapping("/group/{slug}")
    public String displayGroup(@PathVariable String slug, Model model) {
        Content group = contentService.findByPathname('/' + ContentType.GROUP.toHyphenatedString() + '/' + slug, ContentType.GROUP);
        Group detail = (Group) group.getDetail();

        detail.setDescription(htmlSanitizationService.sanitizeHtml(detail.getDescription().replace("\n", "<br/>")));

        model.addAttribute("group", group);
        model.addAttribute("adminCount", group.getAdministrators().size());
        model.addAttribute("memberCount", group.getParticipants().size());

        try {
            String userId = userService.getUserId();
            model.addAttribute("isMember", group.getParticipants().contains(userId));
            model.addAttribute("isAdmin", group.getAdministrators().contains(userId));
        } catch (UserNotAuthenticatedException e) {
            model.addAttribute("isMember", false);
            model.addAttribute("isAdmin", false);
        }

        return "groups/group-page";
    }

    @PostMapping("/join/group")
    @ResponseBody
    public ResponseEntity<String> joinGroup(@RequestParam("contentId") String contentId) {
        interactionService.addParticipant(contentId);
        return ResponseEntity.ok("Success");
    }

    @PostMapping("/leave/group")
    public ResponseEntity<String> leaveGroup(@RequestParam("contentId") String contentId) {
        interactionService.removeParticipant(contentId);
        return ResponseEntity.ok("Success");
    }

    @PostMapping("/email/group")
    @ResponseBody
    public ResponseEntity<String> emailGroup(@RequestBody @Valid EmailRequest emailRequest) {
        boolean response = notificationService.email(emailRequest);

        if (response) {
            return ResponseEntity.ok("Email sent successfully!");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email. Please try again.");
        }
    }

    private GroupDto createDto(Content content) {
        if (!(content.getDetail() instanceof Group groupDetail)) {
            throw new IllegalArgumentException("ContentDto is not a GroupDto");
        }

        GroupDto group = new GroupDto();
        group.setContentId(content.getId());
        group.setPathname(content.getPathname());
        group.setTitle(groupDetail.getTitle());
        group.setDescription(groupDetail.getDescription());
        group.setTags(content.getTags());

        return group;
    }
}
