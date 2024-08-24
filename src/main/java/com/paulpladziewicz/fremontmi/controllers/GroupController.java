package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Announcement;
import com.paulpladziewicz.fremontmi.models.Group;
import com.paulpladziewicz.fremontmi.models.GroupDetailsDto;
import com.paulpladziewicz.fremontmi.services.GroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/groups")
    public String displayGroups(Model model) {
        model.addAttribute("groups", groupService.findAll());
        return "groups/groups";
    }

    @GetMapping("/groups/{id}")
    public String displayGroup(@PathVariable String id, Model model) {
        model.addAttribute("group", groupService.findGroupById(id));
        return "groups/group-page";
    }

    @PostMapping("/groups/join")
    public String joinGroup(@RequestParam("groupId") String groupId) {
        groupService.joinGroup(groupId);

        // return htmx
        return "redirect:/groups/";
    }

    @PostMapping("/groups/leave")
    public String leaveGroup(@RequestParam("groupId") String groupId) {
        groupService.leaveGroup(groupId);

        // return htmx
        return "redirect:/groups";
    }

    @GetMapping("/my/groups")
    public String groups(Model model) {
        try {
            List<GroupDetailsDto> groupDetails = groupService.findGroupsByUser();
            model.addAttribute("groups", groupDetails);
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
        return "groups/htmx/group-announcements-form";
    }

    @PostMapping("/announcements/group/{groupId}")
    public String addGroupAnnouncement(@NotNull @PathVariable String groupId, @Valid Announcement announcement, Model model) {
        groupService.addAnnouncement(groupId, announcement);
        model.addAttribute("group", groupService.findGroupById(groupId));
        return "groups/htmx/group-announcements";
    }

    @PutMapping("/announcements/group/{groupId}")
    public String updateGroupAnnouncement(@NotNull @PathVariable String groupId, @Valid Announcement announcement, BindingResult result, Model model) {
        groupService.updateAnnouncement(groupId, announcement);
        model.addAttribute("group", groupService.findGroupById(groupId));
        return "groups/htmx/group-announcements-form";
    }

    @PostMapping("/delete/group/announcement")
    public String deleteGroupAnnouncement(@NotNull @RequestParam("groupId") String groupId, @NotNull @RequestParam("announcementId") String announcementId) {
        groupService.deleteAnnouncement(groupId, Integer.parseInt(announcementId));
        return "redirect:/groups";
    }
}
