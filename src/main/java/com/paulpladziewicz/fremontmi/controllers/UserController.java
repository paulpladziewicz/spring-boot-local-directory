package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.UserRegistrationDto;
import com.paulpladziewicz.fremontmi.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.validation.BindingResult;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login () {
        return "login";
    }

    @GetMapping("/logout")
    public String logout () {
        return "login";
    }

    @GetMapping("/register")
    public String register () {
        return "register";
    }

    @PostMapping("/register")
    public String handleRegistration (@ModelAttribute UserRegistrationDto userRegistrationDto, BindingResult result) {
        if (result.hasErrors()) {
            // Handle errors
            return "register";
        }

        userService.createUser(userRegistrationDto);

        return "redirect:/events";
    }
}
