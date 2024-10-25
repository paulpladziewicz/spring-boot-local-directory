package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.exceptions.UserNotAuthenticatedException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.HtmlSanitizationService;
import com.paulpladziewicz.fremontmi.services.TagService;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class BusinessController {

    private final HtmlSanitizationService htmlSanitizationService;
    private final BusinessService businessService;
    private final TagService tagService;
    private final UserService userService;

    public BusinessController(HtmlSanitizationService htmlSanitizationService, BusinessService businessService, TagService tagService, UserService userService) {
        this.htmlSanitizationService = htmlSanitizationService;
        this.businessService = businessService;
        this.tagService = tagService;
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

        Business savedBusiness = businessService.create(business);

        return "redirect:/businesses/" + savedBusiness.getSlug();
    }

    @GetMapping("/businesses")
    public String displayActiveBusinesses(@RequestParam(value = "tag", required = false) String tag, Model model) {
        List<Business> businesses = businessService.findAll(tag);

        List<Content> contentList = new ArrayList<>(businesses);
        List<TagUsage> popularTags = tagService.getTagUsageFromContent(contentList, 15);
        model.addAttribute("popularTags", popularTags);
        model.addAttribute("selectedTag", tag);

        model.addAttribute("businesses", businesses);

        return "businesses/businesses";
    }

    @GetMapping("/businesses/{slug}")
    public String viewBusiness(@PathVariable String slug, Model model) {
        Business business = businessService.findBySlug(slug);

        business.setDescription(htmlSanitizationService.sanitizeHtml(business.getDescription().replace("\n", "<br/>")));

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
        List<Business> businesses = businessService.findAllByUser();

        model.addAttribute("businesses", businesses);

        return "businesses/my-businesses";
    }

    @GetMapping("/edit/business/{slug}")
    public String editBusiness(@PathVariable String slug, Model model) {
        Business business = businessService.findBySlug(slug);

        String tagsAsString = String.join(",", business.getTags());
        model.addAttribute("tagsAsString", tagsAsString);

        model.addAttribute("business", business);

        return "businesses/edit-business";
    }

    @PostMapping("/edit/business")
    public String updateBusiness(@Valid @ModelAttribute("business") Business business, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("tagsAsString", String.join(",", business.getTags()));
            return "businesses/edit-business";
        }

        Business updatedBusiness = businessService.update(business);

        model.addAttribute("business", updatedBusiness);

        return "redirect:/businesses/" + updatedBusiness.getSlug();
    }

    @PostMapping("/delete/business")
    public String deleteBusiness(@RequestParam("businessId") String businessId) {
        businessService.delete(businessId);

        return "redirect:/my/businesses";
    }

    @PostMapping("/contact/business")
    public ResponseEntity<Map<String, Object>> handleContactForm(
            @RequestBody ContactFormRequest contactFormRequest) {

        Boolean contactFormSuccess = businessService.handleContactFormSubmission(
                contactFormRequest.getSlug(),
                contactFormRequest.getName(),
                contactFormRequest.getEmail(),
                contactFormRequest.getMessage()
        );

        Map<String, Object> response = new HashMap<>();

        if (!contactFormSuccess) {
            response.put("success", false);
            response.put("message", "An error occurred while trying to send your message. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        response.put("success", true);
        response.put("message", "We've passed your message along! We hope you hear back soon.");
        return ResponseEntity.ok(response);
    }
}
