package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.services.EmailService;
import com.paulpladziewicz.fremontmi.services.NotificationService;
import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final EmailService emailService;

    private final NotificationService notificationService;

    @Value("${stripe.publishable.key}")
    private String stripePublicKey;

    public HomeController(EmailService emailService, NotificationService notificationService) {
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

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

    @PostMapping("/contact")
    public String handleContactForm(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("message") String message,
            RedirectAttributes redirectAttributes) {

        emailService.sendContactUsEmailAsync(name, email, message);

        redirectAttributes.addFlashAttribute("successMessage", "Thank you for reaching out. We will get back to you shortly.");

        return "redirect:/#contact-us";
    }

    @PostMapping("/subscribe")
    public String handleSubscribeForm(
            @RequestParam("email") @Email String email,
            RedirectAttributes redirectAttributes) {

        notificationService.subscribe(email);

        redirectAttributes.addFlashAttribute("subscribedMessage", "Thank you for subscribing to updates.");

        return "redirect:/#subscribe";
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
