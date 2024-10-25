package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.exceptions.UserNotAuthenticatedException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class GroupController {

    private final HtmlSanitizationService htmlSanitizationService;

    private final ContentService contentService;

    private final UserService userService;

    public GroupController(HtmlSanitizationService htmlSanitizationService, ContentService contentService, InteractionService interactionService, NotificationService notificationService, UserService userService) {
        this.htmlSanitizationService = htmlSanitizationService;
        this.contentService = contentService;
        this.userService = userService;
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

        Content createdGroup = contentService.create(ContentType.GROUP, group);

        return "redirect:" + createdGroup.getPathname();
    }

    @GetMapping("/groups")
    public String displayGroups(@RequestParam(value = "tag", required = false) String tag, Model model) {
        List<Content> groups;
        if (tag != null && !tag.isEmpty()) {
            groups = contentService.findByTagAndType(tag, ContentType.GROUP);

        } else {
            groups = contentService.findByType(ContentType.GROUP);
        }

        model.addAttribute("groups", groups);
        return "groups/groups";
    }

    @GetMapping("/group/{slug}")
    public String displayGroup(@PathVariable String slug, Model model) {
        Content group = contentService.findByPathname('/' + ContentType.GROUP.getContentType() + '/' + slug, ContentType.GROUP);
        Group groupDetail = (Group) group.getDetail();

        groupDetail.setDescription(htmlSanitizationService.sanitizeHtml(groupDetail.getDescription().replace("\n", "<br/>")));
        group.setDetail(groupDetail);

        model.addAttribute("group", group);
        model.addAttribute("adminCount", group.getAdministrators().size());
        model.addAttribute("memberCount", groupDetail.getMembers().size());

        try {
            String userId = userService.getUserId();
            model.addAttribute("isMember", groupDetail.getMembers().contains(userId));
            model.addAttribute("isAdmin", group.getAdministrators().contains(userId));
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
        Content group = contentService.findByPathname('/' + ContentType.GROUP.getContentType() + '/' + slug, ContentType.GROUP);

        String tagsAsString = String.join(",", group.getDetail().getTags());
        model.addAttribute("tagsAsString", tagsAsString);

        model.addAttribute("group", group);

        return "groups/edit-group";
    }

    @PostMapping("/edit/group")
    public String updateGroup(@NotNull @RequestParam("contentId") String contentId, @ModelAttribute("group") @Valid Group group, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", group.getTags()));
            return "groups/edit-group";
        }

        Content updatedGroup = contentService.update(contentId, group);

        return "redirect:" + updatedGroup.getPathname();
    }

    @PostMapping("/delete/group")
    public String deleteGroup(@RequestParam(value = "contentId") String contentId) {
        contentService.delete(contentId);

        return "redirect:/my/groups";
    }
}
