package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.exceptions.UserNotAuthenticatedException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.ContentService;
import com.paulpladziewicz.fremontmi.services.HtmlSanitizationService;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class BusinessController {

    private final HtmlSanitizationService htmlSanitizationService;
    private final ContentService contentService;
    private final UserService userService;

    public BusinessController(HtmlSanitizationService htmlSanitizationService, ContentService contentService, UserService userService) {
        this.htmlSanitizationService = htmlSanitizationService;
        this.contentService = contentService;
        this.userService = userService;
    }

    @GetMapping("/businesses")
    public String displayActiveBusinesses() {
        return "spa";
    }

    @GetMapping("/business/{slug}")
    public String viewBusiness(@PathVariable String slug, Model model) {
        Content content = contentService.findByPathname('/' + ContentType.BUSINESS.toHyphenatedString() + '/' + slug, ContentType.BUSINESS);
        Business detail = (Business) content.getDetail();

        detail.setDescription(htmlSanitizationService.sanitizeHtml(detail.getDescription().replace("\n", "<br/>")));

        String userId;
        try {
           userId = userService.getUserId();
        } catch (UserNotAuthenticatedException e) {
            model.addAttribute("isAdmin", false);
            model.addAttribute("business", content);
            return "businesses/business-page";
        }

        boolean isAdmin = content.getAdministrators().contains(userId);

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("business", content);

        return "businesses/business-page";
    }

    private BusinessDto createDto(Content content) {
        if (!(content.getDetail() instanceof Business business)) {
            throw new IllegalArgumentException("ContentDto is not a BusinessDto");
        }

        BusinessDto dto = new BusinessDto();
        dto.setContentId(content.getId());
        dto.setPathname(content.getPathname());
        dto.setTitle(business.getTitle());
        dto.setHeadline(business.getHeadline());
        dto.setDescription(business.getDescription());
        dto.setTags(content.getTags());
        dto.setAddress(business.getAddress());
        dto.setDisplayAddress(business.isDisplayAddress());
        dto.setPhoneNumber(business.getPhoneNumber());
        dto.setDisplayPhoneNumber(business.isDisplayPhoneNumber());
        dto.setEmail(business.getEmail());
        dto.setDisplayEmail(business.isDisplayEmail());
        dto.setWebsite(business.getWebsite());

        return dto;
    }
}
