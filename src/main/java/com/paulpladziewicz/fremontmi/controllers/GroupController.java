package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.GroupService;
import com.paulpladziewicz.fremontmi.services.HtmlSanitizationService;
import com.paulpladziewicz.fremontmi.services.TagService;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import java.util.Optional;

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
            return "groups/create-group";
        }

        ServiceResponse<Group> createGroupResponse = groupService.createGroup(group);

        if (createGroupResponse.hasError()) {
            model.addAttribute("error", true);
            model.addAttribute("group", group);
            return "groups/create-group";
        }

        Content createdGroup = createGroupResponse.value();

        redirectAttributes.addFlashAttribute("successMessage", "Group created successfully!");

        return "redirect:/groups/" + createdGroup.getSlug();
    }

    @GetMapping("/groups")
    public String displayGroups(@RequestParam(value = "tag", required = false) String tag, Model model) {
        ServiceResponse<List<Group>> findAllResponse = groupService.findAll(tag);

        if (findAllResponse.hasError()) {
            model.addAttribute("error", true);
        }

        List<Group> groups = findAllResponse.value();
        model.addAttribute("groups", groups);

        List<Content> contentList = new ArrayList<>(groups);
        List<TagUsage> popularTags = tagService.getTagUsageFromContent(contentList, 15);
        model.addAttribute("popularTags", popularTags);
        model.addAttribute("selectedTag", tag);

        return "groups/groups";
    }

    @GetMapping("/groups/{slug}")
    public String displayGroup(@PathVariable String slug, Model model) {
        ServiceResponse<Group> findBySlugResponse = groupService.findBySlug(slug);

        if (findBySlugResponse.hasError()) {
            model.addAttribute("error", true);
            return "groups/group-page";
        }

        Group group = findBySlugResponse.value();

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
    public String joinGroup(@RequestParam("slug") String slug, Model model) {
        ServiceResponse<Boolean> serviceResponse = groupService.joinGroup(slug);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "groups/group-page";
        }

        return "redirect:/groups/" + slug;
    }

    @PostMapping("/groups/leave")
    public String leaveGroup(@RequestParam("slug") String slug, Model model) {
        ServiceResponse<Boolean> serviceResponse = groupService.leaveGroup(slug);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "groups/group-page";
        }

        return "redirect:/groups/" + slug;
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


    @GetMapping("/edit/group/{slug}")
    public String getEditGroupForm(@NotNull @PathVariable String slug, Model model) {
        ServiceResponse<Group> serviceResponse = groupService.findBySlug(slug);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "groups/edit-group";
        }

        Group group = serviceResponse.value();

        // Convert the list of tags to a comma-separated string
        String tagsAsString = String.join(",", group.getTags());

        model.addAttribute("group", group);
        model.addAttribute("tagsAsString", tagsAsString);  // Add the comma-separated string to the model

        return "groups/edit-group";
    }

    @PostMapping("/edit/group")
    public String updateGroup(@ModelAttribute("group") @Valid Group group, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "groups/edit-group";
        }
        ServiceResponse<Content> serviceResponse = groupService.updateGroup(group);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "groups/edit-group";
        }

        Content updatedGroup = serviceResponse.value();

        redirectAttributes.addFlashAttribute("isSuccess", true);

        return "redirect:/groups/" + updatedGroup.getSlug();
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

    @PostMapping("/email/group")
    @ResponseBody
    public ResponseEntity<String> handleEmailGroup(@RequestBody EmailGroupRequest emailGroupRequest) {
        boolean emailSent = groupService.emailGroup(
                emailGroupRequest.getSlug(),
                emailGroupRequest.getSubject(),
                emailGroupRequest.getMessage()
        );

        if (emailSent) {
            return ResponseEntity.ok("Email sent successfully!");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email. Please try again.");
        }
    }
}
