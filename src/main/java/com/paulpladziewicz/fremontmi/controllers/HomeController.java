package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.services.EmailService;
import com.paulpladziewicz.fremontmi.services.SubscribeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final EmailService emailService;

    private final SubscribeService subscribeService;

    public HomeController(EmailService emailService, SubscribeService subscribeService) {
        this.emailService = emailService;
        this.subscribeService = subscribeService;
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
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {

        subscribeService.subscribe(email);

        redirectAttributes.addFlashAttribute("subscribedMessage", "Thank you for subscribing to updates.");

        return "redirect:/#subscribe";
    }

    @GetMapping("/test")
    public String test () {
        return "test";
    }
}
