package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.ContactFormRequest;
import com.paulpladziewicz.fremontmi.services.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContactFormController {

    private final EmailService emailService;

    public ContactFormController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/article-contact")
    public ResponseEntity<String> submitContactForm(@RequestBody ContactFormRequest request) {
        try {
            emailService.sendContactUsEmailAsync(request.getName(), request.getEmail(), "Parks article submission: " + request.getMessage());

            return ResponseEntity.ok("Form submitted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to submit the form");
        }
    }

    @PostMapping("/taqueria")
    public ResponseEntity<String> taqueriaContactForm(@RequestBody ContactFormRequest contactFormRequest) {
        try {
            emailService.generalContactForm("ppladziewicz@gmail.com",
                    contactFormRequest.getName(),
                    contactFormRequest.getEmail(),
                    contactFormRequest.getMessage());

            return ResponseEntity.ok("Form submitted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to submit the form");
        }
    }
}
