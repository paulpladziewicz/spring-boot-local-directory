package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Business;
import com.paulpladziewicz.fremontmi.models.PaymentRequest;
import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.services.BusinessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping("/create/business/overview")
    public String createBusinessListingOverview(Model model) {
        return "businesses/business-create-overview";
    }

    @PostMapping("/create/business/form")
    public String createBusinessListingView(@RequestParam("priceId") String priceId, @RequestParam(value = "businessId", required = false) String existingBusinessId, Model model) {
        // shows outstanding businesses to continue with or resub
        // option to create a new business
        // form post back to the same to setup the form, whether with an existing business or new business
        // following the pricing choice in the beginning of the flow
        if (priceId == null || priceId.isEmpty() || priceId.trim().isEmpty()) {
            model.addAttribute("error", "Pricing appears to be tampered with. Please refresh the page and try again.");
            return "businesses/business-create-overview";
        }

        if (!priceId.equals("price_1Pv7V0BCHBXtJFxOinfPKMUE") && !priceId.equals("price_1Pv7XIBCHBXtJFxOUIvRA6Xf")) {
            model.addAttribute("error", "Pricing appears to be tampered with. Please refresh the page and try again.");
            return "businesses/business-create-overview";
        }

        if (existingBusinessId != null && !existingBusinessId.isEmpty() && !existingBusinessId.trim().isEmpty()) {

            Optional<Business> optionalBusiness = businessService.findBusinessById(existingBusinessId);

            if (optionalBusiness.isEmpty()) {
                model.addAttribute("errorMessage", "Business not found. Please refresh the page and try again.");
                return "businesses/business-create-overview";
            }

            Business existingBusiness = optionalBusiness.get();

            existingBusiness.setSubscriptionPriceId(priceId);

            model.addAttribute("business", existingBusiness);
            return "businesses/business-create-form";
        }

        ServiceResponse<List<Business>> findBusinessesByUserResponse = businessService.findBusinessesByUser();

        if (findBusinessesByUserResponse.hasError()) {
            model.addAttribute("error", true);
            return "businesses/business-create-overview";
        }

        List<Business> businesses = findBusinessesByUserResponse.value();
        List<Business> incompleteBusinesses = new ArrayList<>();

        for (Business business : businesses) {
            if (business.getStatus().equals("incomplete")) {
                incompleteBusinesses.add(business);
            }
        }

        if (!incompleteBusinesses.isEmpty()) {
            model.addAttribute("priceId", priceId);
            model.addAttribute("incompleteBusinesses", incompleteBusinesses);
            return "businesses/business-continue-progress";
        }

        Business business = new Business();
        business.setSubscriptionPriceId(priceId);

        model.addAttribute("business", business);

        return "businesses/business-create-form";
    }

    @PostMapping("/create/business/subscription")
    public String createBusinessListing(@Valid @ModelAttribute("business") Business business, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "businesses/business-create-form";
        }

        String priceId = business.getSubscriptionPriceId();

        if (priceId == null || priceId.isEmpty() || priceId.trim().isEmpty()) {
            model.addAttribute("error", "Pricing appears to be tampered with. Please refresh the page and try again.");
            return "businesses/business-create-overview";
        }

        if (!priceId.equals("price_1Pv7V0BCHBXtJFxOinfPKMUE") && !priceId.equals("price_1Pv7XIBCHBXtJFxOUIvRA6Xf")) {
            model.addAttribute("error", "Pricing appears to be tampered with. Please refresh the page and try again.");
            return "businesses/business-create-overview";
        }

        ServiceResponse<Business> serviceResponse = businessService.createBusiness(business);

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "events/create-event";
        }

        Business savedBusiness = serviceResponse.value();

        model.addAttribute("business", savedBusiness);
        model.addAttribute("planName", "Monthly Business Listing Subscription");
        model.addAttribute("price", "$10/month");

        return "businesses/business-create-subscription";
    }

    @PostMapping("/create/business/subscription/success")
    @ResponseBody
    public ResponseEntity<?> handleSubscriptionSuccess(@RequestBody PaymentRequest paymentRequest) {
        ServiceResponse<Business> serviceResponse = businessService.handleSubscriptionSuccess(paymentRequest);

        if (serviceResponse.hasError()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", serviceResponse.errorCode()));
        }

        Business business = serviceResponse.value();

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("redirectUrl", "/businesses/" + business.getId());

        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/businesses")
    public String displayActiveBusinesses(Model model) {
        ServiceResponse<List<Business>> serviceResponse = businessService.findAllActiveBusinesses();

        if (serviceResponse.hasError()) {
            model.addAttribute("error", true);
            return "businesses/business-listing-overview";
        }

        List<Business> businesses = serviceResponse.value();

        model.addAttribute("businesses", businesses);

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

    @GetMapping("/edit/business/{id}")
    public String editBusiness(@PathVariable String id, Model model) {
        Optional<Business> businessOptional = businessService.findBusinessById(id);

        if (businessOptional.isEmpty()) {
            model.addAttribute("error", true);
            return "businesses/businesses";
        }

        Business business = businessOptional.get();

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

        return "businesses/business-page";
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
}
