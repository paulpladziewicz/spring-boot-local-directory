package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UserService userService;

    public DashboardController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/my/**")
    public String dashboard() {
        String user = userService.getSignedInUser();
        System.out.println(user);
        return "dashboard";
    }
}
