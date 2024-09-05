package com.paulpladziewicz.fremontmi.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class NeighborServiceController {

    @GetMapping("/neighbor-services")
    public String displayNeighborServices(Model model) {
        return "neighborservices/neighborservices";
    }

    @GetMapping("/neighbor-service/{id}")
    public String viewNeighborService(@PathVariable String id, Model model) {
        return "neighborservices/neighborservices-page";
    }
}
