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

    @GetMapping("/create/group")
    public String displayCreateForm(Model model) {
        model.addAttribute("group", new Group());
        return "groups/create-group";
    }

    @PostMapping("/create/group")
    public String createGroup(@ModelAttribute("group") @Valid GroupDto group, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "groups/create-group";
        }

        Content createdGroup = contentService.create(ContentType.GROUP, group);

        return "redirect:" + createdGroup.getPathname();
    }

    @GetMapping("/groups")
    public String displayGroups(@RequestParam(value = "tag", required = false) String tag, @RequestParam(defaultValue = "0") int page, Model model) {
        Page<Content> groups;
        if (tag != null && !tag.isEmpty()) {
            groups = contentService.findByTagAndType(tag, ContentType.GROUP, page);

        } else {
            groups = contentService.findByType(ContentType.GROUP, page);
        }

        model.addAttribute("groups", groups);
        return "groups/groups";
    }

    @GetMapping("/group/{slug}")
    public String displayGroup(@PathVariable String slug, Model model) {
        Content content = contentService.findByPathname('/' + ContentType.GROUP.getContentType() + '/' + slug, ContentType.GROUP);
        GroupDto group = createDto(content);

        group.setDescription(htmlSanitizationService.sanitizeHtml(group.getDescription().replace("\n", "<br/>")));

        model.addAttribute("group", group);
        model.addAttribute("adminCount", content.getAdministrators().size());
        model.addAttribute("memberCount", content.getParticipants().size());

        try {
            String userId = userService.getUserId();
            model.addAttribute("isMember", content.getParticipants().contains(userId));
            model.addAttribute("isAdmin", content.getAdministrators().contains(userId));
        } catch (UserNotAuthenticatedException e) {
            model.addAttribute("isMember", false);
            model.addAttribute("isAdmin", false);
        }

        return "groups/group-page";
    }

    @GetMapping("/my/groups")
    public String groups(Model model) {
        List<Content> groups = contentService.findByUserAndType(ContentType.GROUP);

        model.addAttribute("groups", groups);

        return "groups/my-groups";
    }

    @GetMapping("/edit/group/{slug}")
    public String getEditGroupForm(@PathVariable String slug, Model model) {
        Content content = contentService.findByPathname('/' + ContentType.GROUP.getContentType() + '/' + slug, ContentType.GROUP);
        GroupDto group = createDto(content);

        String tagsAsString = String.join(",", group.getTags());
        model.addAttribute("tagsAsString", tagsAsString);

        model.addAttribute("group", group);

        return "groups/edit-group";
    }

    @PostMapping("/edit/group")
    public String updateGroup(@ModelAttribute("group") @Valid GroupDto group, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", group.getTags()));
            return "groups/edit-group";
        }

        Content updatedGroup = contentService.update(group.getContentId(), group);

        return "redirect:" + updatedGroup.getPathname();
    }

    @PostMapping("/delete/group")
    public String deleteGroup(@RequestParam(value = "contentId") String contentId) {
        contentService.delete(contentId);

        return "redirect:/my/groups";
    }

    @PostMapping("/join/group")
    public String joinGroup(@RequestParam("contentId") String contentId) {
        interactionService.addParticipant(contentId);
        // start with json
        return "redirect:/groups";
    }

    @PostMapping("/leave/group")
    public String leaveGroup(@RequestParam("contentId") String contentId) {
        interactionService.removeParticipant(contentId);
        // start with json
        return "redirect:/groups";
    }

    @PostMapping("/email/group")
    @ResponseBody
    public ResponseEntity<String> handleEmailGroup(@RequestBody EmailGroupRequest emailGroupRequest) {
        boolean response = notificationService.emailParticipants(emailGroupRequest);

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

        return group;
    }
}
