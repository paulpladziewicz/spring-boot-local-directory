package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.UserRepository;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Email;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
public class UserController {

    private final UserService userService;

    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("userRegistrationDto", new UserRegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegistration(@ModelAttribute("userRegistrationDto") @Valid UserRegistrationDto userRegistrationDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("userRegistrationDto", userRegistrationDto);
            return "auth/register";
        }

        userRegistrationDto.setTermsAcceptedAt(LocalDateTime.now());

        try {
            userService.createUser(userRegistrationDto);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("username")) {
                result.rejectValue("username", "error.userRegistrationDto", e.getMessage());
            } else if (e.getMessage().contains("email")) {
                result.rejectValue("email", "error.userRegistrationDto", e.getMessage());
            } else {
                result.rejectValue("matchingPassword", "error.userRegistrationDto", e.getMessage());
            }
            return "auth/register";
        }

        return "redirect:login";
    }

    @GetMapping("/my/settings")
    public String settings(Model model) {
        model.addAttribute("userDetails", userService.getUserDetails());
        return "settings";
    }

    @PostMapping("/my/settings")
    public String updateSettings(@ModelAttribute("userDetails") @Valid UserDetailsDto userDetailsDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("userDetails", userDetailsDto);
            return "settings";
        }
        UserDetailsDto updatedUserDetails = userService.updateUserDetails(userDetailsDto);
        model.addAttribute("userDetails", updatedUserDetails);
        model.addAttribute("isSuccess", true);
        return "settings";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        model.addAttribute("emailDto", new EmailDto());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@ModelAttribute("emailDto") @Valid EmailDto emailDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("emailDto", emailDto);
            return "auth/forgot-password";
        }

        userService.forgotPassword(emailDto.getEmail());

        model.addAttribute("tokenSent", true);

        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword(Model model, @RequestParam("token") String token) {
        ResetPasswordDto resetPasswordDto = new ResetPasswordDto();
        resetPasswordDto.setToken(token);
        model.addAttribute("resetPasswordDto", resetPasswordDto);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordConfirmation(@ModelAttribute("resetPasswordDto") ResetPasswordDto resetPasswordDto, Model model) {
        userService.resetPassword(resetPasswordDto);

        model.addAttribute("message", "Password has been reset successfully");
        return "auth/login";
    }

    @GetMapping("/forgot-username")
    public String forgotUsername() {
        return "auth/forgot-username";
    }

    @PostMapping("/forgot-username")
    public String handleForgotUsername(@RequestParam("email") @Email String email, Model model) {
        // Check if the email is valid
        if (email == null || email.isEmpty()) {
            model.addAttribute("emailError", "Please provide a valid email address.");
            return "auth/forgot-username";
        }

        // Attempt to send the username to the email
        userService.forgotUsername(email);

//        if (!emailSent) {
//            model.addAttribute("emailError", "No account found with that email address.");
//            return "auth/forgot-username";
//        }

        // Redirect to the home page or a success page
        model.addAttribute("message", "An email has been sent with your username.");
        return "auth/login"; // Or redirect to a specific page
    }
}

