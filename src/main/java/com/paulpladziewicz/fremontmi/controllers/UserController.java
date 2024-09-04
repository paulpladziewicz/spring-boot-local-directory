package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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

        ServiceResult<Void> serviceResult = userService.createUser(userRegistrationDto);

        if (!serviceResult.isSuccess()) {
            switch (serviceResult.getErrorCode()) {
                case "username_exists":
                    result.rejectValue("username", "error.userRegistrationDto", serviceResult.getMessage());
                    break;
                case "email_exists":
                    result.rejectValue("email", "error.userRegistrationDto", serviceResult.getMessage());
                    break;
                case "database_error":
                    model.addAttribute("databaseError", "There was a problem accessing the database. Please try again later.");
                    break;
                case "password_mismatch":
                    result.rejectValue("matchingPassword", "error.userRegistrationDto", serviceResult.getMessage());
                    break;
                default:
                    model.addAttribute("unexpectedError", "An unexpected error occurred. Please try again later.");
                    break;
            }
            model.addAttribute("userRegistrationDto", userRegistrationDto);
            return "auth/register";
        }

        return "redirect:/login";
    }

    @GetMapping("/my/settings")
    public String settings(Model model) {
        Optional<UserProfile> userProfile = userService.getUserProfile();

        if (userProfile.isEmpty()) {
            model.addAttribute("error", true);
            return "settings";
        }

        model.addAttribute("userProfile", userProfile.get());

        return "settings";
    }

    @PostMapping("/my/settings")
    public String updateSettings(@ModelAttribute("userDetails") @Valid UserProfile userProfile, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("userDetails", userProfile);
            return "settings";
        }
        ServiceResult<UserProfile> updatedUserProfile = userService.updateUserProfile(userProfile);

        if (updatedUserProfile.isSuccess()) {
            model.addAttribute("isSuccess", true);
            model.addAttribute("userDetails", updatedUserProfile.getData());
            return "settings";
        }

        model.addAttribute("isSuccess", false);
        model.addAttribute("userDetails", userProfile);
        return "settings";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        model.addAttribute("emailDto", new EmailDto());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@ModelAttribute("emailDto") @Valid EmailDto emailDto, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("emailDto", emailDto);
            return "auth/forgot-password";
        }

        if (userService.forgotPassword(emailDto.getEmail()).isSuccess()) {
            redirectAttributes.addFlashAttribute("tokenSent", true);
            return "redirect:/login";
        }

        model.addAttribute("isSuccess", false);
        model.addAttribute("emailDto", emailDto);
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
    public String resetPasswordConfirmation(@ModelAttribute("resetPasswordDto") ResetPasswordDto resetPasswordDto,BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }

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

