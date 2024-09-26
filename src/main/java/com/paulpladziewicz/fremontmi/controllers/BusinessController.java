package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.BusinessService;
import com.paulpladziewicz.fremontmi.services.TagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class BusinessController {

    private final BusinessService businessService;

    private final TagService tagService;

    public BusinessController(BusinessService businessService, TagService tagService) {
        this.businessService = businessService;
        this.tagService = tagService;
    }

    @GetMapping("/create/business/overview")
    public String createBusinessListingOverview(Model model) {
        return "businesses/create-business-overview";
    }

    @PostMapping("/setup/create/business")
    public String setupCreateBusinessForm(@RequestParam("priceId") String priceId, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("priceId", priceId);
        return "redirect:/create/business";
    }

    @GetMapping("/create/business")
    public String createBusinessListingView(Model model) {
        if (model.containsAttribute("priceId")) {
            String priceId = (String) model.getAttribute("priceId");

            Business business = new Business();
            business.setPriceId(priceId);

            model.addAttribute("business", business);

            return "businesses/create-business";
        } else {
            return "redirect:/create/business/overview";
        }
    }

    @PostMapping("/create/business")
    public String createBusinessListing(@Valid @ModelAttribute("business") Business business, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "businesses/create-business";
        }

        ServiceResponse<Business> createBusinessResponse = businessService.createBusiness(business);

        if (createBusinessResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to create the business listing. Please try again later.");
            return "businesses/create-business";
        }

        Business savedBusiness = createBusinessResponse.value();

        return "redirect:/pay/subscription?contentId=" + savedBusiness.getId();
    }

    @GetMapping("/businesses")
    public String displayActiveBusinesses(@RequestParam(value = "tag", required = false) String tag, Model model) {
        ServiceResponse<List<Business>> serviceResponse = businessService.findAllBusinesses(tag);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "businesses/create-business-overview";
        }

        List<Business> businesses = serviceResponse.value();

        List<Content> contentList = new ArrayList<>(businesses);
        List<TagUsage> popularTags = tagService.getTagUsageFromContent(contentList, 15);
        model.addAttribute("popularTags", popularTags);
        model.addAttribute("selectedTag", tag);

        model.addAttribute("businesses", businesses);

        return "businesses/businesses";
    }

    @GetMapping("/businesses/{slug}")
    public String viewBusiness(@PathVariable String slug, Model model) {
        Optional<Business> businessOptional = businessService.findBusinessBySlug(slug);

        if (businessOptional.isEmpty()) {
            model.addAttribute("error", true);
            return "businesses/businesses";
        }

        Business business = businessOptional.get();

        // TODO add isAdmin attribute to model
        model.addAttribute("business", business);

        return "businesses/business-page";
    }

    @GetMapping("/my/businesses")
    public String getMyBusinesses(Model model) {
        ServiceResponse<List<Business>> serviceResponse = businessService.findBusinessesByUser();

        if (serviceResponse.hasError()) {
            model.addAttribute("generalError", true);
            return "businesses/businesses";
        }

        List<Business> businesses = serviceResponse.value();

        model.addAttribute("businesses", businesses);

        return "businesses/my-businesses";
    }

    @GetMapping("/edit/business/{slug}")
    public String editBusiness(@PathVariable String slug, Model model) {
        Optional<Business> businessOptional = businessService.findBusinessBySlug(slug);

        if (businessOptional.isEmpty()) {
            model.addAttribute("error", true);
            return "businesses/businesses";
        }

        Business business = businessOptional.get();

        String tagsAsString = String.join(",", business.getTags());
        model.addAttribute("tagsAsString", tagsAsString);

        model.addAttribute("business", business);

        return "businesses/edit-business";
    }

    @PostMapping("/edit/business")
    public String updateBusiness(@Valid @ModelAttribute("business") Business business, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "businesses/edit-business";
        }

        ServiceResponse<Business> serviceResponse = businessService.updateBusiness(business);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "businesses/edit-business";
        }

        Business updatedBusiness = serviceResponse.value();

        model.addAttribute("business", updatedBusiness);

        return "redirect:/businesses/" + updatedBusiness.getSlug();
    }

    @PostMapping("/delete/business")
    public String deleteBusiness(@RequestParam("businessId") String businessId, Model model) {
        ServiceResponse<Boolean> serviceResponse = businessService.deleteBusiness(businessId);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "businesses/my-businesses";
        }

        return "redirect:/my/businesses";
    }

    @PostMapping("/contact/business")
    public ResponseEntity<Map<String, Object>> handleContactForm(
            @RequestBody ContactFormRequest contactFormRequest) {

        ServiceResponse<Boolean> contactFormSubmissionResponse = businessService.handleContactFormSubmission(
                contactFormRequest.getSlug(),
                contactFormRequest.getName(),
                contactFormRequest.getEmail(),
                contactFormRequest.getMessage()
        );

        Map<String, Object> response = new HashMap<>();

        if (contactFormSubmissionResponse.hasError()) {
            response.put("success", false);
            response.put("message", "An error occurred while trying to send your message. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        response.put("success", true);
        response.put("message", "Thank you for reaching out. We will get back to you shortly.");
        return ResponseEntity.ok(response);
    }
}
