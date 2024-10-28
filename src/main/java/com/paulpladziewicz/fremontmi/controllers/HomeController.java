package com.paulpladziewicz.fremontmi.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @Value("${stripe.publishable.key}")
    private String stripePublicKey;

    @GetMapping("/")
    public String home () {
        return "home";
    }

    @GetMapping("/tagging-guidelines")
    public String getTags(Model model) {
        return "tagging-guidelines";
    }

    @GetMapping("/error")
    public String error () {
        return "error";
    }

    @GetMapping("/pay/subscription")
    public String paySubscription(Model model) {
        model.addAttribute("stripePublicKey", stripePublicKey);
        return "stripe/pay-subscription";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy() {
        return "legal/privacy-policy";
    }

    @GetMapping("/terms-of-service")
    public String termsOfService() {
        return "legal/terms-of-service";
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Healthy");
    }
}
