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


    private final UserService userService;

    public NeighborServicesProfileController(HtmlSanitizationService htmlSanitizationService, ContentService contentService, UserService userService) {
        this.htmlSanitizationService = htmlSanitizationService;
        this.contentService = contentService;
        this.userService = userService;
    }

    @GetMapping("/overview/neighbor-services")
    public String getOverview(Model model) {
        return "neighborservices/overview";
    }


    @GetMapping("/neighbor-services")
    public String displayActiveNeighborServices() {
        return "spa";
    }

    @GetMapping("/neighbor-services-profile/{slug}")
    public String viewNeighborService(@PathVariable String slug, Model model) {
        Content content = contentService.findByPathname('/' + ContentType.NEIGHBOR_SERVICES_PROFILE.toHyphenatedString() + '/' + slug, ContentType.NEIGHBOR_SERVICES_PROFILE);
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
