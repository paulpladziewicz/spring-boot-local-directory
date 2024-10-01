package com.paulpladziewicz.fremontmi.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ArticleController {

    @GetMapping("/articles")
    public String articles(Model model) {
        return "articles/static-articles";
    }

    @GetMapping("/articles/parks")
    public String parks(Model model) {
        return "articles/parks";
    }
}
