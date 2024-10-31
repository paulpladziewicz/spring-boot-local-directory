package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.exceptions.UserNotAuthenticatedException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.ContentService;
import com.paulpladziewicz.fremontmi.services.HtmlSanitizationService;
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
public class BusinessController {

    private final HtmlSanitizationService htmlSanitizationService;
    private final ContentService contentService;
    private final UserService userService;

    public BusinessController(HtmlSanitizationService htmlSanitizationService, ContentService contentService, UserService userService) {
        this.htmlSanitizationService = htmlSanitizationService;
        this.contentService = contentService;
        this.userService = userService;
    }

    @GetMapping("/create/business")
    public String createBusinessListingView(Model model) {
        Business business = new Business();

        model.addAttribute("business", business);

        return "businesses/create-business";
    }

    @PostMapping("/create/business")
    public String createBusinessListing(@Valid @ModelAttribute("business") BusinessDto businessDto, Model model, BindingResult result) {
        if (result.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", businessDto.getTags()));
            return "businesses/create-business";
        }

        Content savedBusiness = contentService.create(ContentType.BUSINESS, businessDto);

        return "redirect:" + savedBusiness.getPathname();
    }

    @GetMapping("/businesses")
    public String displayActiveBusinesses(@RequestParam(value = "tag", required = false) String tag, @RequestParam(defaultValue = "0") int page,  Model model) {
        Page<Content> businesses;
        if (tag != null && !tag.isEmpty()) {
            businesses = contentService.findByTagAndType(tag, ContentType.BUSINESS, page);

        } else {
            businesses = contentService.findByType(ContentType.BUSINESS, page);
        }

        model.addAttribute("businesses", businesses);
        model.addAttribute("businessListings", businesses.getContent());

        return "businesses/businesses";
    }

    @GetMapping("/business/{slug}")
    public String viewBusiness(@PathVariable String slug, Model model) {
        Content content = contentService.findByPathname('/' + ContentType.BUSINESS.name().toLowerCase() + '/' + slug, ContentType.BUSINESS);
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

    @GetMapping("/my/businesses")
    public String getMyBusinesses(Model model) {
        List<Content> businesses = contentService.findByUserAndType(ContentType.BUSINESS);

        model.addAttribute("businesses", businesses);

        return "businesses/my-businesses";
    }

    @GetMapping("/edit/business/{slug}")
    public String editBusiness(@PathVariable String slug, Model model) {
        Content content = contentService.findByPathname('/' + ContentType.BUSINESS.name().toLowerCase() + '/' + slug, ContentType.BUSINESS);
        BusinessDto business = createDto(content);

        String tagsAsString = String.join(",", business.getTags());
        model.addAttribute("tagsAsString", tagsAsString);

        model.addAttribute("business", business);

        return "businesses/edit-business";
    }

    @PostMapping("/edit/business")
    public String updateBusiness(@NotNull @RequestParam("contentId") String contentId, @Valid @ModelAttribute("business") BusinessDto businessDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", businessDto.getTags()));
            return "businesses/edit-business";
        }

        Content updatedBusiness = contentService.update(contentId, businessDto);

        model.addAttribute("business", updatedBusiness);

        return "redirect:" + updatedBusiness.getPathname();
    }

    @PostMapping("/delete/business")
    public String deleteBusiness(@RequestParam("businessId") String businessId) {
        contentService.delete(businessId);

        return "redirect:/my/businesses";
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
