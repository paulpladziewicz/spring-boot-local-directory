package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Announcement;
import com.paulpladziewicz.fremontmi.models.Group;
import com.paulpladziewicz.fremontmi.models.SendEmailDto;
import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.services.EmailService;
import com.paulpladziewicz.fremontmi.services.GroupService;
import com.paulpladziewicz.fremontmi.services.HtmlSanitizationService;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Controller
public class GroupController {

    private final HtmlSanitizationService htmlSanitizationService;

    private final GroupService groupService;

    private final UserService userService;

    public GroupController(HtmlSanitizationService htmlSanitizationService, GroupService groupService, UserService userService) {
        this.htmlSanitizationService = htmlSanitizationService;
        this.groupService = groupService;
        this.userService = userService;
    }

    @GetMapping("/create/group")
    public String displayCreateForm(Model model) {
        model.addAttribute("group", new Group());
        return "groups/create-group";
    }

    @PostMapping("/create/group")
    public String createGroup(@ModelAttribute("group") @Valid Group group, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "groups/create-group";
        }

        ServiceResponse<Group> serviceResponse = groupService.addGroup(group);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            model.addAttribute("group", group);
            return "groups/create-group";
        }

        Group savedGroup = serviceResponse.value();

        redirectAttributes.addFlashAttribute("successMessage", "Group created successfully!");

        return "redirect:/groups/" + savedGroup.getId();
    }

    @GetMapping("/groups")
    public String displayGroups(Model model) {
        ServiceResponse<List<Group>> serviceRequest = groupService.findAll();

        if (serviceRequest.hasError()) {
            model.addAttribute("error", true);
        }

        List<Group> groups = serviceRequest.value();
        model.addAttribute("groups", groups);

        return "groups/groups";
    }

    @GetMapping("/groups/{id}")
    public String displayGroup(@PathVariable String id, Model model) {
        ServiceResponse<Group> serviceResponse = groupService.findGroupById(id);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "groups/group-page";
        }

        Group group = serviceResponse.value();
        group.setDescription(htmlSanitizationService.sanitizeHtml(group.getDescription().replace("\n", "<br/>")));

        model.addAttribute("group", group);
        model.addAttribute("adminCount", group.getAdministrators().size());
        model.addAttribute("memberCount", group.getMembers().size());

        Optional<String> userId = userService.getUserId();

        if (userId.isPresent()) {
            model.addAttribute("isMember", group.getMembers().contains(userId.get()));
            model.addAttribute("isAdmin", group.getAdministrators().contains(userId.get()));
        } else {
            model.addAttribute("isMember", false);
            model.addAttribute("isAdmin", false);
        }

        return "groups/group-page";
    }

    @PostMapping("/groups/join")
    public String joinGroup(@RequestParam("groupId") String groupId, Model model) {
        ServiceResponse<Boolean> serviceResponse = groupService.joinGroup(groupId);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "groups/group-page";
        }

        return "redirect:/groups/" + groupId;
    }

    @PostMapping("/groups/leave")
    public String leaveGroup(@RequestParam("groupId") String groupId, Model model) {
        ServiceResponse<Boolean> serviceResponse = groupService.leaveGroup(groupId);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "groups/group-page";
        }

        return "redirect:/groups/" + groupId;
    }

    @GetMapping("/my/groups")
    public String groups(Model model) {
        ServiceResponse<List<Group>> serviceRequest = groupService.findGroupsByUser();

        if (serviceRequest.hasError()) {
            model.addAttribute("error", true);
            return "groups/my-groups";
        }

        List<Group> groups = serviceRequest.value();

        model.addAttribute("groups", groups);

        return "groups/my-groups";
    }


    @GetMapping("/edit/group/{id}")
    public String getEditGroupForm(@NotNull @PathVariable String id, Model model) {
        ServiceResponse<Group> serviceResponse = groupService.findGroupById(id);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "groups/edit-group";
        }

        Group group = serviceResponse.value();

        model.addAttribute("group", group);
        return "groups/edit-group";
    }

    @PostMapping("/edit/group")
    public String updateGroup(@ModelAttribute("group") @Valid Group group, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "groups/edit-group";
        }
        ServiceResponse<Group> serviceResponse = groupService.updateGroup(group);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "groups/edit-group";
        }

        Group updatedGroup = serviceResponse.value();

        redirectAttributes.addFlashAttribute("isSuccess", true);

        return "redirect:/groups/" + updatedGroup.getId();
    }

    @PostMapping("/delete/group")
    public String deleteGroup(@NotNull @RequestParam("groupId") String groupId, RedirectAttributes redirectAttributes) {
        ServiceResponse<Void> serviceResponse = groupService.deleteGroup(groupId);

        if (serviceResponse.hasError()) {
            redirectAttributes.addFlashAttribute("error", true);
            return "redirect:/groups/" + groupId;
        }

        return "redirect:/groups";
    }

    @GetMapping("/announcements/group/{groupId}")
    public String getGroupAnnouncementHtml(@NotNull @PathVariable String groupId, Model model) {
        ServiceResponse<Group> serviceResponse = groupService.findGroupById(groupId);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "groups/edit-group";
        }

        Group group = serviceResponse.value();

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

        ServiceResponse<List<Announcement>> serviceResponse = groupService.addAnnouncement(groupId, announcement);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "groups/edit-group";
        }

        ServiceResponse<Group> secondServiceResponse = groupService.findGroupById(groupId);

        if (secondServiceResponse.hasError()) {
            model.addAttribute("error", true);
            return "groups/edit-group";
        }

        Group group = secondServiceResponse.value();

        model.addAttribute("group", group);
        return "groups/htmx/group-announcements";
    }

    @PostMapping("/delete/group/announcement")
    public String deleteGroupAnnouncement(@NotNull @RequestParam("groupId") String groupId, @NotNull @RequestParam("announcementId") String announcementId, Model model) {
        ServiceResponse<Boolean> serviceResponse = groupService.deleteAnnouncement(groupId, Integer.parseInt(announcementId));

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
        }

        return "groups/htmx/delete";
    }
//
//    @GetMapping("/group/email/{emailTarget}/{groupId}")
//    public String emailGroup(@NotNull @PathVariable String emailTarget, @NotNull @PathVariable String groupId, Model model) {
//        model.addAttribute("emailTarget", emailTarget);
//        model.addAttribute("groupId", groupId);
//        return "groups/email-group";
//    }
//
//    @PostMapping("/group/email/send")
//    public String handleEmailGroup(@NotNull @RequestParam("emailTarget") String emailTarget,
//                                   @NotNull @RequestParam("groupId") String groupId,
//                                   @NotBlank @RequestParam("subject") String subject,
//                                   @NotBlank @RequestParam("message") String message,
//                                   Model model) {
//        boolean emailSent = groupService.emailGroup(emailTarget, groupId, subject, message);
//
//        if (emailSent) {
//            model.addAttribute("successMessage", "Email sent successfully!");
//        } else {
//            model.addAttribute("errorMessage", "Failed to send email. Please try again.");
//        }
//
//        model.addAttribute("emailTarget", emailTarget);
//        model.addAttribute("groupId", groupId);
//        return "groups/email-group";
//    }
}
