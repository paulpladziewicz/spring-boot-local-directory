package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.exceptions.UserNotAuthenticatedException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.GroupService;
import com.paulpladziewicz.fremontmi.services.HtmlSanitizationService;
import com.paulpladziewicz.fremontmi.services.TagService;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Controller
public class GroupController {

    private final HtmlSanitizationService htmlSanitizationService;

    private final GroupService groupService;

    private final UserService userService;

    private final TagService tagService;

    public GroupController(HtmlSanitizationService htmlSanitizationService, GroupService groupService, UserService userService, TagService tagService) {
        this.htmlSanitizationService = htmlSanitizationService;
        this.groupService = groupService;
        this.userService = userService;
        this.tagService = tagService;
    }

    @GetMapping("/create/group")
    public String displayCreateForm(Model model) {
        model.addAttribute("group", new Group());
        return "groups/create-group";
    }

    @PostMapping("/create/group")
    public String createGroup(@ModelAttribute("group") @Valid Group group, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", group.getTags()));
            return "groups/create-group";
        }

        Group createdGroup = groupService.createGroup(group);

        redirectAttributes.addFlashAttribute("successMessage", "Group created successfully!");

        return "redirect:/groups/" + createdGroup.getSlug();
    }

    @GetMapping("/groups")
    public String displayGroups(@RequestParam(value = "tag", required = false) String tag, Model model) {
        List<Group> groups = groupService.findAll(tag);
        model.addAttribute("groups", groups);

        List<Content> contentList = new ArrayList<>(groups);
        List<TagUsage> popularTags = tagService.getTagUsageFromContent(contentList, 15);
        model.addAttribute("popularTags", popularTags);
        model.addAttribute("selectedTag", tag);

        return "groups/groups";
    }

    @GetMapping("/groups/{slug}")
    public String displayGroup(@PathVariable String slug, Model model) {
        Group group = groupService.findBySlug(slug);

        group.setDescription(htmlSanitizationService.sanitizeHtml(group.getDescription().replace("\n", "<br/>")));

        model.addAttribute("group", group);
        model.addAttribute("adminCount", group.getAdministrators().size());
        model.addAttribute("memberCount", group.getMembers().size());

        try {
            String userId = userService.getUserId();
            model.addAttribute("isMember", group.getMembers().contains(userId));
            model.addAttribute("isAdmin", group.getAdministrators().contains(userId));
        } catch (UserNotAuthenticatedException e) {
            model.addAttribute("isMember", false);
            model.addAttribute("isAdmin", false);
        }

        return "groups/group-page";
    }

    @PostMapping("/groups/join")
    public String joinGroup(@RequestParam("slug") String slug) {
        groupService.joinGroup(slug);

        return "redirect:/groups/" + slug;
    }

    @PostMapping("/groups/leave")
    public String leaveGroup(@RequestParam("slug") String slug) {
        groupService.leaveGroup(slug);

        return "redirect:/groups/" + slug;
    }

    @GetMapping("/my/groups")
    public String groups(Model model) {
        List<Group> groups = groupService.findGroupsByUser();

        model.addAttribute("groups", groups);

        return "groups/my-groups";
    }


    @GetMapping("/edit/group/{slug}")
    public String getEditGroupForm(@NotNull @PathVariable String slug, Model model) {
        Group group = groupService.findBySlug(slug);

        String tagsAsString = String.join(",", group.getTags());
        model.addAttribute("tagsAsString", tagsAsString);

        model.addAttribute("group", group);

        return "groups/edit-group";
    }

    @PostMapping("/edit/group")
    public String updateGroup(@ModelAttribute("group") @Valid Group group, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", group.getTags()));
            return "groups/edit-group";
        }

        Group updatedGroup = groupService.updateGroup(group);

        redirectAttributes.addFlashAttribute("isSuccess", true);

        return "redirect:/groups/" + updatedGroup.getSlug();
    }

    @PostMapping("/delete/group")
    public String deleteGroup(@NotNull @RequestParam("groupId") String groupId) {
        groupService.deleteGroup(groupId);

        return "redirect:/groups";
    }

    @GetMapping("/announcements/group/{groupId}")
    public String getGroupAnnouncementHtml(@NotNull @PathVariable String groupId, Model model) {
        Group group = groupService.findGroupById(groupId);

        model.addAttribute("group", group);

        return "groups/htmx/group-announcements";
    }

    @GetMapping("/announcements/group/form/{groupId}")
    public String getGroupAnnouncementForm(@NotNull @PathVariable String groupId, Model model) {
        model.addAttribute("groupId", groupId);
        model.addAttribute("announcement", new Announcement());
        return "groups/htmx/group-announcements-form";
    }

    @PostMapping("/announcements/group/{groupId}")
    public String addGroupAnnouncement(@NotNull @PathVariable String groupId, @Valid Announcement announcement, Model model) {
        announcement.setCreationDate(Instant.now());

        groupService.addAnnouncement(groupId, announcement);
        // TODO not performant
        Group group = groupService.findGroupById(groupId);

        model.addAttribute("group", group);
        return "groups/htmx/group-announcements";
    }

    @PostMapping("/delete/group/announcement")
    public String deleteGroupAnnouncement(@NotNull @RequestParam("groupId") String groupId, @NotNull @RequestParam("announcementId") String announcementId) {
        groupService.deleteAnnouncement(groupId, Integer.parseInt(announcementId));

        return "groups/htmx/delete";
    }

    @PostMapping("/email/group")
    @ResponseBody
    public ResponseEntity<String> handleEmailGroup(@RequestBody EmailGroupRequest emailGroupRequest) {
        Boolean response = groupService.emailGroup(
                emailGroupRequest.getSlug(),
                emailGroupRequest.getSubject(),
                emailGroupRequest.getMessage()
        );

        if (response) {
            return ResponseEntity.ok("Email sent successfully!");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email. Please try again.");
        }
    }
}
