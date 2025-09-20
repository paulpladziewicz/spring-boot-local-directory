package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.app.ServiceResponse;
import com.paulpladziewicz.fremontmi.user.UserProfile;
import com.paulpladziewicz.fremontmi.user.UserService;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SettingsController {

    private final UserService userService;

    public SettingsController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/my/settings")
    public String settings(Model model) {
        model.addAttribute("userProfile", userService.getUserProfile());

        return "settings/profile";
    }


    @PostMapping("/my/settings")
    public String updateSettings(@ModelAttribute("userProfile") @Valid UserProfile userProfile, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("userProfile", userProfile);
            return "settings/profile";
        }
        ServiceResponse<UserProfile> serviceResponse = userService.updateUserProfile(userProfile);

        if (serviceResponse.hasError()) {
            model.addAttribute("isSuccess", false);
            model.addAttribute("userProfile", userProfile);
            return "settings/profile";
        }

        model.addAttribute("isSuccess", true);
        model.addAttribute("userProfile", serviceResponse.value());

        return "settings/profile";
    }

    @GetMapping("/my/settings/subscriptions")
    public String subscriptionSettings() {
        return "settings/subscriptions";
    }

    @GetMapping("/my/settings/billing")
    public String billingSettings() {
        return "settings/billing";
    }
}
