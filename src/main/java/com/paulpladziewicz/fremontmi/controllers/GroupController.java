package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Announcement;
import com.paulpladziewicz.fremontmi.models.Group;
import com.paulpladziewicz.fremontmi.models.SendEmailDto;
import com.paulpladziewicz.fremontmi.services.EmailService;
import com.paulpladziewicz.fremontmi.services.GroupService;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Controller
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;

    public GroupController(GroupService groupService, UserService userService) {
        this.groupService = groupService;
        this.userService = userService;
    }

    @GetMapping("/groups")
    public String displayGroups(Model model) {
        model.addAttribute("groups", groupService.findAll());
        return "groups/groups";
    }

    @GetMapping("/groups/{id}")
    public String displayGroup(@PathVariable String id, Model model) {
        Group group = groupService.findGroupById(id);
        String userId = userService.getUserId();
        model.addAttribute("group", group);
        model.addAttribute("isMember", group.getMembers().contains(userId));
        model.addAttribute("isAdmin", group.getAdministrators().contains(userId));
        model.addAttribute("adminCount", group.getAdministrators().size());
        model.addAttribute("memberCount", group.getMembers().size());
        return "groups/group-page";
    }

    @PostMapping("/groups/join")
    public String joinGroup(@RequestParam("groupId") String groupId, Model model) {
        groupService.joinGroup(groupId);
        model.addAttribute("groupId", groupId);
        return "redirect:/groups/" + groupId;
    }

    @PostMapping("/groups/leave")
    public String leaveGroup(@RequestParam("groupId") String groupId) {
        System.out.println(groupId);
        groupService.leaveGroup(groupId);
        return "redirect:/groups/" + groupId;
    }

    @GetMapping("/my/groups")
    public String groups(Model model) {
        try {
            List<Group> groups = groupService.findGroupsByUser();
            model.addAttribute("groups", groups);
            return "groups/my-groups";
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return "home";
        }
    }

    @GetMapping("/create/group")
    public String displayCreateForm(Model model) {
        model.addAttribute("group", new Group());
        return "groups/create-group";
    }

    @PostMapping("/create/group")
    public String createGroup(@ModelAttribute("group") @Valid Group group, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "groups/create-group";
        }

        Group savedGroup = groupService.addGroup(group);

        model.addAttribute("successMessage", "Group created successfully!");
        return "redirect:/groups/" + savedGroup.getId();
    }

    @GetMapping("/edit/group/{id}")
    public String getEditGroupForm(@NotNull @PathVariable String id, Model model) {
        model.addAttribute("group", groupService.findGroupById(id));
        return "groups/edit-group";
    }

    @PostMapping("/edit/group")
    public String updateGroup(@ModelAttribute("group") @Valid Group group, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "groups/edit-group";
        }
        Group updatedGroup = groupService.updateGroup(group);
        model.addAttribute("group", updatedGroup);
        model.addAttribute("isSuccess", true);
        return "redirect:/groups/" + updatedGroup.getId();
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
        model.addAttribute("group", groupService.findGroupById(groupId));
        return "groups/htmx/group-announcements";
    }

    @PostMapping("/delete/group/announcement")
    public String deleteGroupAnnouncement(@NotNull @RequestParam("groupId") String groupId, @NotNull @RequestParam("announcementId") String announcementId) {
        groupService.deleteAnnouncement(groupId, Integer.parseInt(announcementId));
        return "groups/htmx/delete";
    }

    @GetMapping("/group/email/{emailTarget}/{groupId}")
    public String emailGroup(@NotNull @PathVariable String emailTarget, @NotNull @PathVariable String groupId, Model model) {
        model.addAttribute("emailTarget", emailTarget);
        model.addAttribute("groupId", groupId);
        return "groups/email-group";
    }

    @PostMapping("/group/email/send")
    public String handleEmailGroup(@NotNull @RequestParam("emailTarget") String emailTarget,
                                   @NotNull @RequestParam("groupId") String groupId,
                                   @NotBlank @RequestParam("subject") String subject,
                                   @NotBlank @RequestParam("message") String message,
                                   Model model) {
        boolean emailSent = groupService.emailGroup(emailTarget, groupId, subject, message);

        if (emailSent) {
            model.addAttribute("successMessage", "Email sent successfully!");
        } else {
            model.addAttribute("errorMessage", "Failed to send email. Please try again.");
        }

        model.addAttribute("emailTarget", emailTarget);
        model.addAttribute("groupId", groupId);
        return "groups/email-group";
    }
}
