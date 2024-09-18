package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.NeighborServicesProfileService;
import com.paulpladziewicz.fremontmi.services.TagService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class NeighborServicesProfileController {

    private final NeighborServicesProfileService neighborServicesProfileService;

    private final TagService tagService;

    public NeighborServicesProfileController(NeighborServicesProfileService neighborServicesProfileService, TagService tagService) {
        this.neighborServicesProfileService = neighborServicesProfileService;
        this.tagService = tagService;
    }

    @GetMapping("/create/neighbor-services-profile/overview")
    public String createNeighborServicesProfileOverview(Model model) {
        return "neighborservices/create-neighbor-services-profile-overview";
    }

    @PostMapping("/setup/create/neighbor-services-profile")
    public String setupNeighborServicesProfileForm(@RequestParam("priceId") String priceId, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("priceId", priceId);
        return "redirect:/create/neighbor-services-profile";
    }

    @GetMapping("/create/neighbor-services-profile")
    public String createNeighborServicesProfileForm(Model model) {
        if (model.containsAttribute("priceId")) {
            String priceId = (String) model.getAttribute("priceId");

            NeighborServicesProfile neighborServicesProfile = new NeighborServicesProfile();
            neighborServicesProfile.getNeighborServices().add(new NeighborService());
            neighborServicesProfile.setPriceId(priceId);

            model.addAttribute("neighborServicesProfile", neighborServicesProfile);

            return "neighborservices/create-neighbor-services-profile";
        } else {
            return "redirect:/create/neighbor-services-profile/overview";
        }
    }

    @PostMapping("/create/neighbor-services-profile")
    public String createNeighborServiceSubscription(@Valid @ModelAttribute("neighborService") NeighborServicesProfile neighborServicesProfile, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("neighborService", neighborServicesProfile);
            return "neighborservices/create-neighbor-services-profile";
        }

        ServiceResponse<NeighborServicesProfile> createNeighborServiceProfileResponse = neighborServicesProfileService.createNeighborServiceProfile(neighborServicesProfile);

        if (createNeighborServiceProfileResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to create the neighbor service. Please try again later.");
            return "neighborservices/neighborservices-create-overview";
        }

        NeighborServicesProfile createdNeighborServicesProfile = createNeighborServiceProfileResponse.value();

        return "redirect:/pay/subscription?contentId=" + createdNeighborServicesProfile.getId();
    }

    @GetMapping("/neighbor-services")
    public String displayActiveNeighborServices(@RequestParam(value = "tag", required = false) String tag, Model model) {
        // Fetch filtered profiles by tag
        ServiceResponse<List<NeighborServicesProfile>> profilesResponse = neighborServicesProfileService.findAllActiveNeighborServices(tag);

        if (profilesResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to retrieve the neighbor services. Please try again later.");
            return "neighborservices/neighborservices";
        }

        List<NeighborServicesProfile> profiles = profilesResponse.value();

        // Fetch all available tags
        List<String> allTags = tagService.findAllDistinctTags();

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
        Optional<NeighborServicesProfile> optionalNeighborService = neighborServicesProfileService.findNeighborServiceProfileById(id);

        if (optionalNeighborService.isEmpty()) {
            model.addAttribute("errorMessage", "Neighbor service not found. Please try again later.");
            return "neighborservices/neighborservices-page";
        }

        NeighborServicesProfile neighborServicesProfile = optionalNeighborService.get();

        model.addAttribute("neighborServiceProfile", neighborServicesProfile);

        return "neighborservices/neighborservices-page";
    }

    @GetMapping("/my/neighbor-services/profile")
    public String viewMyNeighborServiceProfile(Model model) {
        Optional<NeighborServicesProfile> optionalNeighborService = neighborServicesProfileService.findNeighborServiceProfileByUserId();

        if (optionalNeighborService.isEmpty()) {
            model.addAttribute("errorMessage", "You do not currently have a NeighborServices™ Profile. Please create one.");
            return "neighborservices/neighborservices-create-overview";
        }

        NeighborServicesProfile neighborServicesProfile = optionalNeighborService.get();

        model.addAttribute("neighborServiceProfile", neighborServicesProfile);

        return "neighborservices/neighborservices-page";
    }

    @GetMapping("/edit/neighbor-service/profile/{id}")
    public String editNeighborServiceProfilePage(@PathVariable String id, Model model) {
        Optional<NeighborServicesProfile> optionalNeighborService = neighborServicesProfileService.findNeighborServiceProfileById(id);

        if (optionalNeighborService.isEmpty()) {
            model.addAttribute("error", true);
            return "neighborservices/neighborservices";
        }

        NeighborServicesProfile neighborServicesProfile = optionalNeighborService.get();

        model.addAttribute("neighborServiceProfile", neighborServicesProfile);

        return "neighborservices/edit-neighborservice";
    }


    @PostMapping("/edit/neighbor-service/profile")
    public String editNeighborService(@Valid @ModelAttribute("neighborService") NeighborServicesProfile neighborServicesProfile, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("neighborService", neighborServicesProfile);
            return "neighborservices/edit-neighborservice";
        }

        // TODO check if the user is authorized to edit the neighbor service

        ServiceResponse<NeighborServicesProfile> updateNeighborServiceResponse = neighborServicesProfileService.saveNeighborServiceProfile(neighborServicesProfile);

        if (updateNeighborServiceResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to update the neighbor service. Please try again later.");
            return "neighborservices/edit-neighborservice";
        }

        NeighborServicesProfile updatedNeighborServicesProfile = updateNeighborServiceResponse.value();

        model.addAttribute("neighborService", updatedNeighborServicesProfile);

        return "redirect:/neighbor-services/" + updatedNeighborServicesProfile.getId();
    }
}
