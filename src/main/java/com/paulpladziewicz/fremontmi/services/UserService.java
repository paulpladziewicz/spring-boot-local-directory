package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.UserProfileRepository;
import com.paulpladziewicz.fremontmi.repositories.UserRepository;
import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final UserProfileRepository userProfileRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public ServiceResult<Void> createUser(UserRegistrationDto userRegistrationDto) {
        try {
            if (userRepository.existsByUsername(userRegistrationDto.getUsername())) {
                return ServiceResult.error("There is already an account with that username: " + userRegistrationDto.getUsername(), "username_exists");
            }

            if (userProfileRepository.existsByEmail(userRegistrationDto.getEmail())) {
                return ServiceResult.error("There is already an account with that email: " + userRegistrationDto.getEmail(), "email_exists");
            }

            validatePasswords(userRegistrationDto.getPassword(), userRegistrationDto.getMatchingPassword());

            UserRecord newUserRecord = new UserRecord();
            newUserRecord.setUsername(userRegistrationDto.getUsername());
            newUserRecord.setPassword(passwordEncoder.encode(userRegistrationDto.getPassword()));
            UserRecord savedUser = userRepository.save(newUserRecord);

            UserProfile userProfile = new UserProfile();
            userProfile.setUserId(savedUser.getUserId());
            userProfile.setEmail(userRegistrationDto.getEmail());
            userProfile.setFirstName(userRegistrationDto.getFirstName());
            userProfile.setLastName(userRegistrationDto.getLastName());
            userProfile.setTermsAcceptedAt(userRegistrationDto.getTermsAcceptedAt());
            UserProfile savedUserProfile = userProfileRepository.save(userProfile);

            emailService.sendWelcomeEmailAsync(userProfile.getEmail());

            logger.info("Successfully created username {} for {} {} with the following email: {}", savedUser.getUsername(), savedUserProfile.getFirstName(), savedUserProfile.getLastName(), userRegistrationDto.getEmail());

            return ServiceResult.success();
        } catch (ValidationException e) {
            return ServiceResult.error("Passwords do not match.", "password_mismatch");
        } catch (DataAccessException e) {
            logger.error("Failed to create user due to database error", e);
            return ServiceResult.error("Failed to create user due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while creating user", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    public List<UserProfile> findAllById(List<String> userIds) {
        try {
            return userProfileRepository.findAllById(userIds);
        } catch (DataAccessException e) {
            logger.error("Database error occurred while retrieving user profiles by IDs", e);
            throw new RuntimeException("Failed to retrieve user profiles due to a database error. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while retrieving user profiles by IDs", e);
            throw new RuntimeException("An unexpected error occurred. Please try again later.");
        }
    }

    public Optional<String> getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            CustomUserDetails customUserDetails = (CustomUserDetails) auth.getPrincipal();
            return Optional.of(customUserDetails.getUserId());
        }

        logger.error("Failed to retrieve userId. User is not authenticated or is anonymous. Authentication object: {}", auth);

        return Optional.empty();
    }

    public Optional<UserProfile> getUserProfile() {
        Optional<String> userId = getUserId();

        return userId.flatMap(userProfileRepository::findById);
    }

    public ServiceResult<Void> saveUserProfile(UserProfile userProfile) {
        try {
            userProfileRepository.save(userProfile);
            return ServiceResult.success();

        } catch (DataAccessException e) {
            logger.error("Failed to save UserProfile due to database error", e);
            return ServiceResult.error("Failed to save user profile due to a database error. Please try again later.", "database_error");

        } catch (Exception e) {
            logger.error("Unexpected error occurred while saving UserProfile", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    public ServiceResult<UserProfile> updateUserProfile(UserProfile updatedUserProfile) {
        try {
            Optional<String> userId = getUserId();
            if (userId.isEmpty()) {
                logger.error("Failed to update user details: userId not found in security context");
                return ServiceResult.error("User not authenticated.", "user_not_authenticated");
            }

            Optional<UserProfile> existingDetails = userProfileRepository.findById(userId.get());
            if (existingDetails.isPresent()) {
                UserProfile updatedDetails = existingDetails.get();
                updatedDetails.setFirstName(updatedUserProfile.getFirstName());
                updatedDetails.setLastName(updatedUserProfile.getLastName());
                updatedDetails.setEmail(updatedUserProfile.getEmail());
                userProfileRepository.save(updatedDetails);
                return ServiceResult.success(updatedDetails);
            } else {
                logger.error("Failed to update user details: no existing user profile found for userId {}", userId.get());
                return ServiceResult.error("User details not found.", "user_not_found");
            }

        } catch (DataAccessException e) {
            logger.error("Database error occurred while updating user details for userId: {}", updatedUserProfile.getUserId(), e);
            return ServiceResult.error("Failed to update user details due to a database error. Please try again later.", "database_error");

        } catch (Exception e) {
            logger.error("Unexpected error occurred while updating user details for userId: {}", updatedUserProfile.getUserId(), e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    public ServiceResult<Void> resetPassword(ResetPasswordDto resetPasswordDto) {
        try {
            String token = resetPasswordDto.getToken();
            String newPassword = resetPasswordDto.getPassword();
            String confirmPassword = resetPasswordDto.getConfirmPassword();

            validatePasswords(newPassword, confirmPassword);

            Optional<UserRecord> user = userRepository.findByResetPasswordToken(token);
            if (user.isEmpty()) {
                logger.error("Failed to reset password: invalid or expired reset token");
                return ServiceResult.error("Invalid or expired reset token.", "invalid_token");
            }

            UserRecord userRecord = user.get();
            userRecord.setPassword(passwordEncoder.encode(newPassword));
            userRecord.setResetPasswordToken(null);
            userRepository.save(userRecord);

            return ServiceResult.success();

        } catch (DataAccessException e) {
            logger.error("Database error occurred while resetting password for token: {}", resetPasswordDto.getToken(), e);
            return ServiceResult.error("Failed to reset password due to a database error. Please try again later.", "database_error");

        } catch (Exception e) {
            logger.error("Unexpected error occurred while resetting password for token: {}", resetPasswordDto.getToken(), e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    public ServiceResult<Void> forgotPassword(String username) {
        try {
            Optional<UserRecord> user = userRepository.findByUsername(username);

            if (user.isEmpty()) {
                logger.warn("Forgot password request: user not found with username {}", username);
                return ServiceResult.error("User not found.", "user_not_found");
            }

            Optional<UserProfile> userProfile = userProfileRepository.findById(user.get().getUserId());

            if (userProfile.isEmpty()) {
                logger.error("UserProfile not found for userId: {}", user.get().getUserId());
                return ServiceResult.error("User profile not found.", "user_profile_not_found");
            }

            String token = UUID.randomUUID().toString();
            UserRecord userRecord = user.get();
            userRecord.setResetPasswordToken(token);
            userRepository.save(userRecord);

            emailService.sendResetPasswordEmailAsync(userProfile.get().getEmail(), "https://fremontmi.com/reset-password?token=" + token);

            return ServiceResult.success();

        } catch (DataAccessException e) {
            logger.error("Database error occurred while processing forgot password for username: {}", username, e);
            return ServiceResult.error("Failed to process forgot password request due to a database error. Please try again later.", "database_error");

        } catch (Exception e) {
            logger.error("Unexpected error occurred while processing forgot password for username: {}", username, e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }


    public ServiceResult<Void> forgotUsername(String email) {
        try {
            Optional<UserProfile> userProfile = userProfileRepository.findByEmail(email);

            if (userProfile.isEmpty()) {
                logger.warn("Forgot username request: no user profile found with email {}", email);
                return ServiceResult.error("No user found with that email.", "email_not_found");
            }

            Optional<UserRecord> user = userRepository.findById(userProfile.get().getUserId());

            if (user.isEmpty()) {
                logger.error("UserRecord not found for userId: {}", userProfile.get().getUserId());
                return ServiceResult.error("User record not found.", "user_record_not_found");
            }

            emailService.sendForgotUsernameEmailAsync(userProfile.get().getEmail(), user.get().getUsername());

            return ServiceResult.success();

        } catch (DataAccessException e) {
            logger.error("Database error occurred while processing forgot username request for email: {}", email, e);
            return ServiceResult.error("Failed to process forgot username request due to a database error. Please try again later.", "database_error");

        } catch (Exception e) {
            logger.error("Unexpected error occurred while processing forgot username request for email: {}", email, e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }


    private void validatePasswords(String password, String matchingPassword) throws ValidationException {
        if (password == null || !password.equals(matchingPassword)) {
            throw new ValidationException("Passwords entered don't match.");
        }
    }
}
