package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.exceptions.ContentNotFoundException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.ContentService;
import com.paulpladziewicz.fremontmi.services.HtmlSanitizationService;
import com.paulpladziewicz.fremontmi.services.TagService;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class NeighborServicesProfileController {

    private final HtmlSanitizationService htmlSanitizationService;

    private final ContentService contentService;

    private final TagService tagService;

    private final UserService userService;

    public NeighborServicesProfileController(HtmlSanitizationService htmlSanitizationService, ContentService contentService, TagService tagService, UserService userService) {
        this.htmlSanitizationService = htmlSanitizationService;
        this.contentService = contentService;
        this.tagService = tagService;
        this.userService = userService;
    }

    @GetMapping("/overview/neighbor-services")
    public String getOverview(Model model) {
        return "neighborservices/overview";
    }

    @GetMapping("/create/neighbor-services-profile")
    public String createNeighborServicesProfileForm(Model model) {
        try {
            List<Content> contentList = contentService.findByUserAndType(ContentType.NEIGHBOR_SERVICES_PROFILE);

            if (contentList.isEmpty()) {
                NeighborServicesProfile neighborServicesProfile = new NeighborServicesProfile();
                neighborServicesProfile.getNeighborServices().add(new NeighborService());

                model.addAttribute("neighborServicesProfile", neighborServicesProfile);

                return "neighborservices/create-neighbor-services-profile";
            }

            return "redirect:/my/neighbor-services-profile";
        } catch (ContentNotFoundException e) {
            NeighborServicesProfile neighborServicesProfile = new NeighborServicesProfile();
            neighborServicesProfile.getNeighborServices().add(new NeighborService());

            model.addAttribute("neighborServicesProfile", neighborServicesProfile);

            return "neighborservices/create-neighbor-services-profile";
        }
    }

    @PostMapping("/create/neighbor-services-profile")
    public String createNeighborServiceSubscription(@Valid @ModelAttribute("neighborService") NeighborServicesProfileDto neighborServicesProfile, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", neighborServicesProfile.getTags()));
            return "neighborservices/create-neighbor-services-profile";
        }

        Content createdNeighborServicesProfile = contentService.create(ContentType.NEIGHBOR_SERVICES_PROFILE, neighborServicesProfile);

        return "redirect:" + createdNeighborServicesProfile.getPathname();
    }

    @GetMapping("/neighbor-services")
    public String displayActiveNeighborServices(@RequestParam(value = "tag", required = false) String tag, @RequestParam(defaultValue = "0") int page,  Model model) {
        Page<Content> profiles;
        if (tag != null && !tag.isEmpty()) {
            profiles = contentService.findByTagAndType(tag, ContentType.NEIGHBOR_SERVICES_PROFILE, page);

        } else {
            profiles = contentService.findByType(ContentType.NEIGHBOR_SERVICES_PROFILE, page);
        }

        List<TagUsage> popularTags = tagService.getTagUsageFromContent(profiles, 15);
        model.addAttribute("popularTags", popularTags);
        model.addAttribute("selectedTag", tag);

        // TODO still displaying profiles that do not have any neighbor services

        model.addAttribute("profiles", profiles);
        model.addAttribute("profilesList", profiles.getContent());

        return "neighborservices/neighbor-services";
    }

    @GetMapping("/neighbor-services-profile/{slug}")
    public String viewNeighborService(@PathVariable String slug, Model model) {
        Content content = contentService.findByPathname('/' + ContentType.NEIGHBOR_SERVICES_PROFILE.getContentType() + '/' + slug, ContentType.NEIGHBOR_SERVICES_PROFILE);
        NeighborServicesProfile  detail = (NeighborServicesProfile) content.getDetail();

        boolean createdByUser;
        try {
            String userId = userService.getUserId();
            createdByUser = Objects.equals(userId, content.getCreatedBy());
        } catch (Exception e) {
            createdByUser = false;
        }

//        if (!createdByUser) {
//            if (Objects.equals(content.getVisibility(), ContentVisibility.RESTRICTED.getVisibility())) {
//                return "restricted-visibility";
//            }
//        }

        detail.setDescription(htmlSanitizationService.sanitizeHtml(detail.getDescription().replace("\n", "<br/>")));

        model.addAttribute("neighborServicesProfile", content);
        model.addAttribute("myProfile", createdByUser);

        return "neighborservices/neighbor-services-profile-page";
    }

    @GetMapping("/my/neighbor-services-profile")
    public String viewMyNeighborServiceProfile(Model model) {
        try {
            List<Content> contentList = contentService.findByUserAndType(ContentType.NEIGHBOR_SERVICES_PROFILE);

            if (contentList.isEmpty()) {
                return "redirect:/create/neighbor-services-profile";
            }

            Content neighborServicesProfile = contentList.getFirst();
            NeighborServicesProfile neighborServicesProfileDetail = (NeighborServicesProfile) neighborServicesProfile.getDetail();

            neighborServicesProfileDetail.setDescription(htmlSanitizationService.sanitizeHtml(neighborServicesProfileDetail.getDescription().replace("\n", "<br/>")));

            model.addAttribute("neighborServicesProfile", neighborServicesProfile);
            model.addAttribute("myProfile", true);
            return "neighborservices/neighbor-services-profile-page";
        } catch (ContentNotFoundException e) {
            return "redirect:/create/neighbor-services-profile";
        }
    }

    @GetMapping("/edit/neighbor-services-profile/{slug}")
    public String editNeighborServiceProfilePage(@PathVariable String slug, Model model) {
        Content content = contentService.findByPathname('/' + ContentType.NEIGHBOR_SERVICES_PROFILE.getContentType() + '/' + slug, ContentType.NEIGHBOR_SERVICES_PROFILE);
        NeighborServicesProfileDto neighborServicesProfile = createDto(content);

        String tagsAsString = String.join(",", neighborServicesProfile.getTags());
        model.addAttribute("tagsAsString", tagsAsString);

        model.addAttribute("neighborServicesProfile", neighborServicesProfile);

        return "neighborservices/edit-neighbor-services-profile";
    }


    @PostMapping("/edit/neighbor-services-profile")
    public String editNeighborService(@NotNull @RequestParam("contentId") String contentId, @Valid @ModelAttribute("neighborService") NeighborServicesProfileDto neighborServicesProfileDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", neighborServicesProfileDto.getTags()));
            return "neighborservices/edit-neighbor-services-profile";
        }

        Content content = contentService.update(contentId, neighborServicesProfileDto);
        NeighborServicesProfileDto neighborServicesProfile = createDto(content);

        model.addAttribute("neighborService", neighborServicesProfile);

        return "redirect:/my/neighbor-services-profile";
    }

    @PostMapping("/delete/neighbor-services-profile")
    public String deleteGroup(@RequestParam(value = "contentId") String contentId) {
        contentService.delete(contentId);

        return "redirect:/neighbor-services";
    }

    private NeighborServicesProfileDto createDto(Content content) {
        if (!(content.getDetail() instanceof NeighborServicesProfile neighborServicesProfile)) {
            throw new IllegalArgumentException("ContentDto is not a NeighborServicesProfileDto");
        }

        NeighborServicesProfileDto dto = new NeighborServicesProfileDto();
        dto.setContentId(content.getId());
        dto.setPathname(content.getPathname());
        dto.setTags(content.getTags());
        dto.setTitle(neighborServicesProfile.getTitle());
        dto.setDescription(neighborServicesProfile.getDescription());
        dto.setEmail(neighborServicesProfile.getEmail());
        dto.setNeighborServices(neighborServicesProfile.getNeighborServices());
        dto.setProfileImageUrl(neighborServicesProfile.getProfileImageUrl());
        dto.setProfileImageFileName(neighborServicesProfile.getProfileImageFileName());

        return dto;
    }
}
