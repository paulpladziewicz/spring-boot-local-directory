package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.UserRegistrationDto;
import com.paulpladziewicz.fremontmi.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

@Controller
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String register (WebRequest request, Model model) {
        logger.info("Registration form requested");
        UserRegistrationDto user = new UserRegistrationDto();
        model.addAttribute("user", user);
        return "register";
    }

    @RequestMapping(value="/register", method= RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String handleRegistration (@RequestBody MultiValueMap<String, String> formData) {
        UserRegistrationDto user = new UserRegistrationDto();
        user.setPassword(formData.getFirst("firstName"));
        user.setPassword(formData.getFirst("lastName"));
        user.setPassword(formData.getFirst("email"));
        user.setPassword(formData.getFirst("password"));
        logger.info("Received the following values {}", user.getPassword());

        userService.registerNewUserAccount(user);
        return "my-events";
    }

    @GetMapping("/login")
    public String loginView () {
        return "login";
    }

    @GetMapping("/forgotPassword")
    public String forgotPassword () {
        return "login";
    }
}
