package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.services.TagService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TagController {

    public final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

//    @GetMapping("/autocomplete/tags")
//    public List<String> getTags(@RequestParam("query") String query) {
//
//        return tagService.autocompleteList(query);
//    }
}
