package com.paulpladziewicz.fremontmi.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalController {

    @GetMapping("/privacy-policy")
    public String privacyPolicy() {
        return "legal/privacy-policy";
    }

    @GetMapping("/terms-of-service")
    public String termsOfService() {
        return "legal/terms-of-service";
    }
}