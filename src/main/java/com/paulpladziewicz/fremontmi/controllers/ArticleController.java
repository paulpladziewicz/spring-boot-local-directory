package com.paulpladziewicz.fremontmi.controllers;

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

    @GetMapping("/create/article")
    public String create(Model model) {
        return "articles/create-article";
    }

    @PostMapping("/uploadFile")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        // Implement file handling logic
        String imageUrl = "/uploads/" + file.getOriginalFilename();
        return ResponseEntity.ok(Map.of("success", 1, "file", Map.of("url", imageUrl)));
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
}
