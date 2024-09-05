package com.paulpladziewicz.fremontmi.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class BusinessController {

    @GetMapping("/businesses/{id}")
    public String viewBusiness(@PathVariable String id, Model model) {
        return "businesses/business-page";
    }
}
