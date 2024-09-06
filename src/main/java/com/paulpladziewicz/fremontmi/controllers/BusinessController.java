package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Business;
import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.services.BusinessService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping("/create/business-listing-overview")
    public String createBusinessListingOverview(Model model) {
        return "businesses/business-create-overview";
    }

    @GetMapping("/create/business-listing")
    public String createBusinessListingView(Model model) {
        model.addAttribute("business", new Business());
        return "businesses/business-create-form";
    }

    @PostMapping("/create/business-listing")
    public String createBusinessListing(@Valid @ModelAttribute("business") Business business, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "businesses/business-create-form";
        }

        ServiceResponse<Business> serviceResponse = businessService.createBusiness(business);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "events/create-event";
        }

        Business savedBusiness = serviceResponse.value();

        return "redirect:/businesses/" + savedBusiness.getId();
    }

    @GetMapping("/businesses")
    public String displayBusinesses(Model model) {
        return "businesses/businesses";
    }

    @GetMapping("/businesses/{id}")
    public String viewBusiness(@PathVariable String id, Model model) {
        Optional<Business> businessOptional = businessService.findBusinessById(id);

        if (businessOptional.isEmpty()) {
            model.addAttribute("error", true);
            return "businesses/businesses";
        }

        Business business = businessOptional.get();

        model.addAttribute("business", business);

        return "businesses/business-page";
    }

    @GetMapping("/stripe/subscribe")
    public String subscribe() {
        return "businesse/business-create-subscription";
    }
}
