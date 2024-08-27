package com.paulpladziewicz.fremontmi.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalController {

    @GetMapping("/privacy-policy")
    public String privacyPolicy(Model model) {
        return "legal/privacy-policy";
    }

    // Terms of Service
    @GetMapping("/terms-of-service")
    public String termsOfService(Model model) {
        return "legal/terms-of-service";
    }
}