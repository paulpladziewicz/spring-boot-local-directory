package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.exceptions.ContentNotFoundException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.ContentService;
import com.paulpladziewicz.fremontmi.services.HtmlSanitizationService;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class NeighborServicesProfileController {

    private final HtmlSanitizationService htmlSanitizationService;

    private final ContentService contentService;

    private final UserService userService;

    public NeighborServicesProfileController(ContentService contentService, HtmlSanitizationService htmlSanitizationService, UserService userService) {
        this.contentService = contentService;
        this.htmlSanitizationService = htmlSanitizationService;
        this.userService = userService;
    }

    @GetMapping("/overview/neighbor-services")
    public String getOverview(Model model) {
        return "neighborservices/overview";
    }

    @GetMapping("/create/neighbor-services-profile")
    public String createNeighborServicesProfileForm(Model model) {
        try {
            contentService.findByUserAndType(ContentType.NEIGHBOR_SERVICES_PROFILE);
            return "redirect:/my/neighbor-services/profile";
        } catch (ContentNotFoundException e) {
            NeighborServicesProfile neighborServicesProfile = new NeighborServicesProfile();
            neighborServicesProfile.getNeighborServices().add(new NeighborService());

            model.addAttribute("neighborServicesProfile", neighborServicesProfile);

            return "neighborservices/create-neighbor-services-profile";
        }
    }

    @PostMapping("/create/neighbor-services-profile")
    public String createNeighborServiceSubscription(@Valid @ModelAttribute("neighborService") NeighborServicesProfile neighborServicesProfile, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", neighborServicesProfile.getTags()));
            return "neighborservices/create-neighbor-services-profile";
        }

        Content createdNeighborServicesProfile = contentService.create(ContentType.NEIGHBOR_SERVICES_PROFILE, neighborServicesProfile);

        return "redirect:" + createdNeighborServicesProfile.getPathname();
    }

    @GetMapping("/neighbor-services")
    public String displayActiveNeighborServices(@RequestParam(value = "tag", required = false) String tag, Model model) {
        List<Content> profiles;
        if (tag != null && !tag.isEmpty()) {
            profiles = contentService.findByTagAndType(tag, ContentType.NEIGHBOR_SERVICES_PROFILE);

        } else {
            profiles = contentService.findByType(ContentType.NEIGHBOR_SERVICES_PROFILE);
        }

        // TODO still displaying profiles that do not have any neighbor services

        model.addAttribute("profiles", profiles);

        return "neighborservices/neighbor-services";
    }

    @GetMapping("/neighbor-services-profile/{slug}")
    public String viewNeighborService(@PathVariable String slug, Model model) {
        Content neighborServicesProfile = contentService.findByPathname('/' + ContentType.NEIGHBOR_SERVICES_PROFILE.getContentType() + '/' + slug);
        NeighborServicesProfile neighborServicesProfileDetail = (NeighborServicesProfile) neighborServicesProfile.getDetail();

        Boolean createdByUser;
        try {
            String userId = userService.getUserId();
            createdByUser = Objects.equals(userId, neighborServicesProfile.getCreatedBy());
        } catch (Exception e) {
            createdByUser = false;
        }

        if (!createdByUser) {
            if (Objects.equals(neighborServicesProfile.getVisibility(), ContentVisibility.RESTRICTED.getVisibility())) {
                return "restricted-visibility";
            }
        }

        neighborServicesProfileDetail.setDescription(htmlSanitizationService.sanitizeHtml(neighborServicesProfileDetail.getDescription().replace("\n", "<br/>")));
        neighborServicesProfile.setDetail(neighborServicesProfileDetail);

        model.addAttribute("neighborServicesProfile", neighborServicesProfile);
        model.addAttribute("myProfile", createdByUser);

        return "neighborservices/neighbor-services-profile-page";
    }

    @GetMapping("/my/neighbor-services-profile")
    public String viewMyNeighborServiceProfile(Model model) {
        try {
            List<Content> contentList = contentService.findByUserAndType(ContentType.NEIGHBOR_SERVICES_PROFILE);
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

    @GetMapping("/edit/neighbor-services-profile")
    public String editNeighborServiceProfilePage(@RequestParam(value = "contentId") String contentId, Model model) {
        Content neighborServicesProfile = contentService.findById(contentId);

        String tagsAsString = String.join(",", neighborServicesProfile.getDetail().getTags());
        model.addAttribute("tagsAsString", tagsAsString);

        model.addAttribute("neighborServicesProfile", neighborServicesProfile);

        return "neighborservices/edit-neighbor-services-profile";
    }


    @PostMapping("/edit/neighbor-services-profile")
    public String editNeighborService(@NotNull @RequestParam("contentId") String contentId, @Valid @ModelAttribute("neighborService") NeighborServicesProfile neighborServicesProfile, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", neighborServicesProfile.getTags()));
            return "neighborservices/edit-neighbor-services-profile";
        }

        Content updatedNeighborServicesProfile = contentService.update(contentId, neighborServicesProfile);

        model.addAttribute("neighborService", updatedNeighborServicesProfile);

        return "redirect:/my/neighbor-services/profile";
    }

    @PostMapping("/delete/neighbor-services-profile")
    public String deleteGroup(@RequestParam(value = "contentId") String contentId) {
        contentService.delete(contentId);

        return "redirect:/neighbor-services";
    }
}
