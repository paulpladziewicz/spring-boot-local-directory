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
        Optional<NeighborServicesProfile> optionalNeighborServicesProfile = neighborServicesProfileService.findNeighborServiceProfileByUserId();

        if (optionalNeighborServicesProfile.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "You already have a NeighborServices™ Profile. You can only have one profile at a time.");
            return "redirect:/my/neighbor-services/profile";

        }

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
            return "neighborservices/create-neighbor-services-profile";
        }

        NeighborServicesProfile createdNeighborServicesProfile = createNeighborServiceProfileResponse.value();

        return "redirect:/pay/subscription?contentId=" + createdNeighborServicesProfile.getId();
    }

    @GetMapping("/neighbor-services")
    public String displayActiveNeighborServices(@RequestParam(value = "tag", required = false) String tag, Model model) {
        ServiceResponse<List<NeighborServicesProfile>> profilesResponse = neighborServicesProfileService.findAllActiveNeighborServices(tag);

        if (profilesResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to retrieve the neighbor services. Please try again later.");
            return "neighborservices/neighbor-services";
        }

        List<NeighborServicesProfile> profiles = profilesResponse.value();

        // TODO still displaying profiles that do not have any neighbor services

        List<Content> contentList = new ArrayList<>(profiles);
        List<TagUsage> popularTags = tagService.getTagUsageFromContent(contentList, 15);
        model.addAttribute("popularTags", popularTags);
        model.addAttribute("selectedTag", tag);

        model.addAttribute("profiles", profiles);

        return "neighborservices/neighbor-services";
    }

    @GetMapping("/neighbor-services/{slug}")
    public String viewNeighborService(@PathVariable String slug, Model model) {
        Optional<NeighborServicesProfile> optionalNeighborService = neighborServicesProfileService.findNeighborServiceProfileBySlug(slug);

        if (optionalNeighborService.isEmpty()) {
            model.addAttribute("errorMessage", "Neighbor service not found. Please try again later.");
            return "redirect:/neighbor-services";
        }

        NeighborServicesProfile neighborServicesProfile = optionalNeighborService.get();

        model.addAttribute("neighborServicesProfile", neighborServicesProfile);

        return "neighborservices/neighbor-services-profile-page";
    }

    @GetMapping("/my/neighbor-services/profile")
    public String viewMyNeighborServiceProfile(Model model) {
        Optional<NeighborServicesProfile> optionalNeighborService = neighborServicesProfileService.findNeighborServiceProfileByUserId();

        if (optionalNeighborService.isEmpty()) {
            model.addAttribute("errorMessage", "You do not currently have a NeighborServices™ Profile. Please create one.");
            return "redirect:/create/neighbor-services-profile/overview";
        }

        NeighborServicesProfile neighborServicesProfile = optionalNeighborService.get();

        model.addAttribute("neighborServicesProfile", neighborServicesProfile);
        model.addAttribute("myProfile", true);

        return "neighborservices/neighbor-services-profile-page";
    }

    @GetMapping("/edit/neighbor-service/profile/{slug}")
    public String editNeighborServiceProfilePage(@PathVariable String slug, Model model) {
        Optional<NeighborServicesProfile> optionalNeighborService = neighborServicesProfileService.findNeighborServiceProfileBySlug(slug);

        if (optionalNeighborService.isEmpty()) {
            model.addAttribute("error", true);
            return "neighborservices/edit-neighbor-services-profile";
        }

        NeighborServicesProfile neighborServicesProfile = optionalNeighborService.get();

        String tagsAsString = String.join(",", neighborServicesProfile.getTags());
        model.addAttribute("tagsAsString", tagsAsString);

        model.addAttribute("neighborServicesProfile", neighborServicesProfile);

        return "neighborservices/edit-neighbor-services-profile";
    }


    @PostMapping("/edit/neighbor-service/profile")
    public String editNeighborService(@Valid @ModelAttribute("neighborService") NeighborServicesProfile neighborServicesProfile, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("neighborService", neighborServicesProfile);
            return "neighborservices/edit-neighbor-services-profile";
        }

        ServiceResponse<NeighborServicesProfile> updateNeighborServiceResponse = neighborServicesProfileService.updateNeighborServiceProfile(neighborServicesProfile);

        if (updateNeighborServiceResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to update the neighbor service. Please try again later.");
            return "neighborservices/edit-neighbor-services-profile";
        }

        NeighborServicesProfile updatedNeighborServicesProfile = updateNeighborServiceResponse.value();

        model.addAttribute("neighborService", updatedNeighborServicesProfile);

        return "redirect:/neighbor-services/" + updatedNeighborServicesProfile.getSlug();
    }
}
