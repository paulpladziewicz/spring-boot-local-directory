package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.services.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
public class ArticleController {

    @Value("${mapbox.secret}")
    private String mapboxSecret;

    private final EmailService emailService;

    public ArticleController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/create/article")
    public String create(Model model) {
        return "articles/create-article";
    }

    @GetMapping("/articles")
    public String articles(Model model) {
        return "articles/static-articles";
    }

    @GetMapping("/articles/parks")
    public String parks(Model model) {
        model.addAttribute("mapboxSecret", mapboxSecret);
        return "articles/parks";
    }

    @GetMapping("/articles/prepare-to-vote")
    public String registerToVote(Model model) {
        return "articles/prepare-to-vote";
    }

    @GetMapping("/articles/coming-soon-taqueria-de-gallo")
    public String ComingSoonTaqueriaDeGallo(Model model) {
        return "articles/coming-soon-taqueria-de-gallo";
    }

    @PostMapping("/taqueria")
    public void taqueriaContactForm() {
        // call email service directly
    }
}
