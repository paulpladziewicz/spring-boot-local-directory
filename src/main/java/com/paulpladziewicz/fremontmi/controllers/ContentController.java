package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.services.ContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/archive/{contentId}")
    public ResponseEntity<String> editBusiness(@PathVariable String contentId) {
        contentService.archive(contentId);
        return ResponseEntity.ok("archived");
    }
}
