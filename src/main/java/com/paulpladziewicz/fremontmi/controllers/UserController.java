package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.EmailDto;
import com.paulpladziewicz.fremontmi.models.ResetPasswordDto;
import com.paulpladziewicz.fremontmi.models.UserRegistrationDto;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    public String register (Model model) {
        model.addAttribute("userRegistrationDto", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegistration (@ModelAttribute("userRegistrationDto") @Valid UserRegistrationDto userRegistrationDto, BindingResult result, Model model, HttpServletRequest request) {
        if (result.hasErrors()) {
            model.addAttribute("userRegistrationDto", userRegistrationDto);
            return "register";
        }

        try {
            userService.createUser(userRegistrationDto);
        } catch (ValidationException e) {
            result.rejectValue("matchingPassword", "Match", e.getMessage());
            return "register";
        }

        model.addAttribute("autoLogin", true);
        model.addAttribute("username", userRegistrationDto.getEmail());
        model.addAttribute("password", userRegistrationDto.getPassword());

        return "login";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword (Model model) {
        model.addAttribute("emailDto", new EmailDto());
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword (@ModelAttribute("emailDto") @Valid EmailDto emailDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("emailDto", emailDto);
            return "forgot-password";
        }

        userService.forgotPassword(emailDto.getEmail());

        model.addAttribute("tokenSent", true);

        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword (Model model) {
        model.addAttribute("resetPasswordDto", new ResetPasswordDto());
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordConfirmation (Model model) {
        model.addAttribute("resetPasswordDto", new ResetPasswordDto());
        return "reset-password";
    }
}
