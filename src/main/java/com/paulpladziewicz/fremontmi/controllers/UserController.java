package com.paulpladziewicz.fremontmi.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {

    @GetMapping("/register")
    public String register () {
        return "login";
    }

    @GetMapping("/login")
    public String loginView () {
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmission () {
        return "login";
    }

    @GetMapping("/forgotPassword")
    public String forgotPassword () {
        return "login";
    }
}
