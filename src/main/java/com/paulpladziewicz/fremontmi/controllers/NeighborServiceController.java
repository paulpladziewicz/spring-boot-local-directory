package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.NeighborServiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class NeighborServiceController {

    private final NeighborServiceService neighborServiceService;

    public NeighborServiceController(NeighborServiceService neighborServiceService) {
        this.neighborServiceService = neighborServiceService;
    }

    @GetMapping("/create/neighbor-service/overview")
    public String createNeighborServiceOverview(Model model) {
        // Need to get the priceIds from the Stripe
        return "neighborservices/neighborservices-create-overview";
    }

    @PostMapping("/create/neighbor-service/form")
    public String createNeighborServiceView(@RequestParam("priceId") String priceId, @RequestParam(value = "neighborServiceProfileId", required = false) String existingNeighborServiceProfileId, Model model) {
        if (existingNeighborServiceProfileId != null && !existingNeighborServiceProfileId.isEmpty() && !existingNeighborServiceProfileId.trim().isEmpty()) {
            Optional<NeighborServiceProfile> optionalNeighborServiceProfile = neighborServiceService.findNeighborServiceProfileById(existingNeighborServiceProfileId);

            if (optionalNeighborServiceProfile.isEmpty()) {
                model.addAttribute("error", "Neighbor service not found. Please try again later.");
                return "neighborservices/neighborservices-create-overview";
            }

            NeighborServiceProfile existingNeighborServiceProfile = optionalNeighborServiceProfile.get();

            model.addAttribute("priceId", priceId);
            model.addAttribute("neighborServiceProfile", existingNeighborServiceProfile);

            return "neighborservices/neighborservices-create-form";
        }

        // TODO provide defaults many fields based on the user's profile

        model.addAttribute("priceId", priceId);
        model.addAttribute("neighborServiceProfile", new NeighborServiceProfile());

        return "neighborservices/neighborservices-create-form";
    }

    @PostMapping("/create/neighbor-service/subscription")
    public String createNeighborServiceSubscription(@RequestParam("priceId") String priceId, @Valid @ModelAttribute("neighborService") NeighborServiceProfile neighborServiceProfile, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("neighborService", neighborServiceProfile);
            return "neighborservices/neighborservices-create-form";
        }

        ServiceResponse<StripeTransactionRecord> createNeighborServiceProfileResponse = neighborServiceService.createNeighborServiceProfile(priceId, neighborServiceProfile);

        if (createNeighborServiceProfileResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to create the neighbor service. Please try again later.");
            return "neighborservices/neighborservices-create-overview";
        }

        StripeTransactionRecord stripeTransactionRecord = createNeighborServiceProfileResponse.value();

        model.addAttribute("clientSecret", stripeTransactionRecord.getClientSecret());
        model.addAttribute("entityId", stripeTransactionRecord.getEntityId());
        model.addAttribute("subscriptionDisplayName", stripeTransactionRecord.getDisplayName());
        model.addAttribute("subscriptionDisplayPrice", stripeTransactionRecord.getDisplayPrice());

        return "neighborservices/neighborservices-create-subscription";
    }

    @PostMapping("/create/neighbor-service/subscription/success")
    @ResponseBody
    public ResponseEntity<?> handleSubscriptionSuccess(@RequestBody PaymentRequest paymentRequest) {
        ServiceResponse<NeighborServiceProfile> serviceResponse = neighborServiceService.handleSubscriptionSuccess(paymentRequest);

        if (serviceResponse.hasError()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", serviceResponse.errorCode()));
        }

        NeighborServiceProfile neighborServiceProfile = serviceResponse.value();

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("redirectUrl", "/neighbor-services/" + neighborServiceProfile.getId());

        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/neighbor-services")
    public String displayActiveNeighborServices(@RequestParam(value = "tag", required = false) String tag, Model model) {
        // Fetch filtered profiles by tag
        ServiceResponse<List<NeighborServiceProfile>> profilesResponse = neighborServiceService.findAllActiveNeighborServices(tag);

        if (profilesResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to retrieve the neighbor services. Please try again later.");
            return "neighborservices/neighborservices";
        }

        List<NeighborServiceProfile> profiles = profilesResponse.value();

        // Fetch all available tags
        List<String> allTags = neighborServiceService.findAllDistinctTags();

        // Extract tags from the currently filtered profiles for display
        Set<String> uniqueTagsForProfiles = profiles.stream()
                .flatMap(profile -> profile.getTags().stream())
                .collect(Collectors.toSet());

        // TODO still displaying profiles that do not have any neighbor services

        model.addAttribute("profiles", profiles);
        model.addAttribute("allTags", allTags);  // All available tags
        model.addAttribute("uniqueTagsForProfiles", uniqueTagsForProfiles);  // Tags for currently displayed profiles
        model.addAttribute("selectedTag", tag);

        return "neighborservices/neighborservices";
    }

    @GetMapping("/neighbor-services/{id}")
    public String viewNeighborService(@PathVariable String id, Model model) {
        Optional<NeighborServiceProfile> optionalNeighborService = neighborServiceService.findNeighborServiceProfileById(id);

        if (optionalNeighborService.isEmpty()) {
            model.addAttribute("errorMessage", "Neighbor service not found. Please try again later.");
            return "neighborservices/neighborservices-page";
        }

        NeighborServiceProfile neighborServiceProfile = optionalNeighborService.get();

        model.addAttribute("neighborServiceProfile", neighborServiceProfile);

        return "neighborservices/neighborservices-page";
    }

    @GetMapping("/my/neighbor-services/profile")
    public String viewMyNeighborServiceProfile(Model model) {
        Optional<NeighborServiceProfile> optionalNeighborService = neighborServiceService.findNeighborServiceProfileByUserId();

        if (optionalNeighborService.isEmpty()) {
            model.addAttribute("errorMessage", "You do not currently have a NeighborServices™ Profile. Please create one.");
            return "neighborservices/neighborservices-create-overview";
        }

        NeighborServiceProfile neighborServiceProfile = optionalNeighborService.get();

        model.addAttribute("neighborServiceProfile", neighborServiceProfile);

        return "neighborservices/neighborservices-page";
    }

    @GetMapping("/edit/neighbor-service/profile/{id}")
    public String editNeighborServiceProfilePage(@PathVariable String id, Model model) {
        Optional<NeighborServiceProfile> optionalNeighborService = neighborServiceService.findNeighborServiceProfileById(id);

        if (optionalNeighborService.isEmpty()) {
            model.addAttribute("error", true);
            return "neighborservices/neighborservices";
        }

        NeighborServiceProfile neighborServiceProfile = optionalNeighborService.get();

        model.addAttribute("neighborServiceProfile", neighborServiceProfile);

        return "neighborservices/edit-neighborservice";
    }


    @PostMapping("/edit/neighbor-service/profile")
    public String editNeighborService(@Valid @ModelAttribute("neighborService") NeighborServiceProfile neighborServiceProfile, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("neighborService", neighborServiceProfile);
            return "neighborservices/edit-neighborservice";
        }

        // TODO check if the user is authorized to edit the neighbor service

        ServiceResponse<NeighborServiceProfile> updateNeighborServiceResponse = neighborServiceService.saveNeighborServiceProfile(neighborServiceProfile);

        if (updateNeighborServiceResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to update the neighbor service. Please try again later.");
            return "neighborservices/edit-neighborservice";
        }

        NeighborServiceProfile updatedNeighborServiceProfile = updateNeighborServiceResponse.value();

        model.addAttribute("neighborService", updatedNeighborServiceProfile);

        return "redirect:/neighbor-services/" + updatedNeighborServiceProfile.getId();
    }
}
