package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.exceptions.UserNotAuthenticatedException;
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
    public String createBusinessListing(@Valid @ModelAttribute("business") Business business, Model model, BindingResult result) {
        if (result.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", business.getTags()));
            return "businesses/create-business";
        }

        Content savedBusiness = contentService.create(ContentType.BUSINESS, business);

        return "redirect:" + savedBusiness.getPathname();
    }

    @GetMapping("/businesses")
    public String displayActiveBusinesses(@RequestParam(value = "tag", required = false) String tag, Model model) {
        List<Content> businesses;
        if (tag != null && !tag.isEmpty()) {
            businesses = contentService.findByTagAndType(tag, ContentType.BUSINESS);

        } else {
            businesses = contentService.findByType(ContentType.BUSINESS);
        }

        model.addAttribute("businesses", businesses);

        return "businesses/businesses";
    }

    @GetMapping("/businesses/{slug}")
    public String viewBusiness(@PathVariable String slug, Model model) {
        Content business = contentService.findByPathname('/' + ContentType.GROUP.getContentType() + '/' + slug);
        Business businessDetail = (Business) business.getDetail();

        businessDetail.setDescription(htmlSanitizationService.sanitizeHtml(businessDetail.getDescription().replace("\n", "<br/>")));
        business.setDetail(businessDetail);

        String userId;
        try {
           userId = userService.getUserId();
        } catch (UserNotAuthenticatedException e) {
            model.addAttribute("isAdmin", false);
            model.addAttribute("business", business);
            return "businesses/business-page";
        }

        boolean isAdmin = business.getAdministrators().contains(userId);

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("business", business);

        return "businesses/business-page";
    }

    @GetMapping("/my/businesses")
    public String getMyBusinesses(Model model) {
        List<Content> businesses = contentService.findByUserAndType(ContentType.BUSINESS);

        model.addAttribute("businesses", businesses);

        return "businesses/my-businesses";
    }

    @GetMapping("/edit/business")
    public String editBusiness(@RequestParam(value = "contentId") String contentId, Model model) {
        Content business = contentService.findById(contentId);

        String tagsAsString = String.join(",", business.getDetail().getTags());
        model.addAttribute("tagsAsString", tagsAsString);

        model.addAttribute("business", business);

        return "businesses/edit-business";
    }

    @PostMapping("/edit/business")
    public String updateBusiness(@NotNull @RequestParam("contentId") String contentId, @Valid @ModelAttribute("business") Business business, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", business.getTags()));
            return "businesses/edit-business";
        }

        Content updatedBusiness = contentService.update(contentId, business);

        model.addAttribute("business", updatedBusiness);

        return "redirect:" + updatedBusiness.getPathname();
    }

    @PostMapping("/delete/business")
    public String deleteBusiness(@RequestParam("businessId") String businessId) {
        contentService.delete(businessId);

        return "redirect:/my/businesses";
    }
}
