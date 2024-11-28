package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Content;
import com.paulpladziewicz.fremontmi.models.ContentType;
import com.paulpladziewicz.fremontmi.services.ContentService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/api/content")
    public ResponseEntity<Page<Content>> getAllContent(
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(defaultValue = "0") int page) {
        Page<Content> content;
        if (contentType != null) {
            content = contentService.findByType(contentType, page);
        } else {
            content = contentService.findLatestContent(page);
        }
        return ResponseEntity.ok(content);
    }

    @GetMapping("/archive/{contentId}")
    public ResponseEntity<String> editBusiness(@PathVariable String contentId) {
        contentService.archive(contentId);
        return ResponseEntity.ok("archived");
    }
}
