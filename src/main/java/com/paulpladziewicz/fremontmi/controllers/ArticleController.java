package com.paulpladziewicz.fremontmi.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class ArticleController {

    @Value("${mapbox.secret}")
    private String mapboxSecret;

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

    @GetMapping("/articles/share-the-joy-give-toys-spread-smiles")
    public String toyDrive(Model model) {
        return "articles/sharing-joy";
    }

    @GetMapping("/articles/cozy-cups")
    public String cozyCups(Model model) {
        return "articles/seasonal-coffee";
    }
}
