package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.services.EmailService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final EmailService emailService;

    public HomeController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/")
    public String home () {
        return "home";
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

        emailService.sendContactUsEmail(name, email, message);

        redirectAttributes.addFlashAttribute("successMessage", "Thank you for reaching out. We will get back to you shortly.");

        return "redirect:/#contact-us";
    }

    @PostMapping("/subscribe")
    public String handleSubscribeForm(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("subscribed", "Thank you for subscribing to updates.");

        return "redirect:/#contact-us";
    }
}
