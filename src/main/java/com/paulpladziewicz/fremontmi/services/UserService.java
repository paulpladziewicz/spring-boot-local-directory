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
    public ServiceResponse<Boolean> createUser(UserRegistrationDto userRegistrationDto) {
        try {
            if (userRepository.existsByUsername(userRegistrationDto.getUsername())) {
                return logAndReturnError("Username already exists.", "username_exists");
            }

            if (userProfileRepository.existsByEmail(userRegistrationDto.getEmail())) {
                return logAndReturnError("Email already exists.", "email_exists");
            }

            validatePasswords(userRegistrationDto.getPassword(), userRegistrationDto.getMatchingPassword());

            UserRecord newUserRecord = createUserRecord(userRegistrationDto);
            UserRecord savedUser = userRepository.save(newUserRecord);

            UserProfile savedUserProfile = createUserProfile(userRegistrationDto, savedUser.getUserId());
            userProfileRepository.save(savedUserProfile);

            emailService.sendWelcomeEmailAsync(userRegistrationDto.getEmail());

            logger.info("Successfully created user {} with email {}", savedUser.getUsername(), savedUserProfile.getEmail());
            return ServiceResponse.value(true);

        } catch (ValidationException e) {
            return logAndReturnError("Password mismatch.", "password_mismatch");
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while creating user.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while creating user.", "unexpected_error", e);
        }
    }

    public ServiceResponse<List<UserProfile>> findAllById(List<String> userIds) {
        try {
            return ServiceResponse.value(userProfileRepository.findAllById(userIds));
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while retrieving user profiles.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while retrieving user profiles.", "unexpected_error", e);
        }
    }

    public Optional<String> getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            CustomUserDetails customUserDetails = (CustomUserDetails) auth.getPrincipal();
            return Optional.of(customUserDetails.getUserId());
        }

        logger.error("Failed to retrieve userId. User is not authenticated.");
        return Optional.empty();
    }

    public Optional<UserProfile> getUserProfile() {
        return getUserId().flatMap(userProfileRepository::findById);
    }

    public ServiceResponse<UserProfile> saveUserProfile(UserProfile userProfile) {
        try {
            return ServiceResponse.value(userProfileRepository.save(userProfile));
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while saving UserProfile.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while saving UserProfile.", "unexpected_error", e);
        }
    }

    public ServiceResponse<UserProfile> updateUserProfile(UserProfile updatedUserProfile) {
        try {
            Optional<String> userId = getUserId();
            if (userId.isEmpty()) {
                return logAndReturnError("User not authenticated.", "user_not_authenticated");
            }

            Optional<UserProfile> existingDetails = userProfileRepository.findById(userId.get());
            if (existingDetails.isEmpty()) {
                return logAndReturnError("User profile not found for userId: " + userId.get(), "user_not_found");
            }

            UserProfile userProfile = existingDetails.get();
            updateProfileDetails(userProfile, updatedUserProfile);

            return ServiceResponse.value(userProfileRepository.save(userProfile));
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while updating UserProfile.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while updating UserProfile.", "unexpected_error", e);
        }
    }

    public ServiceResponse<Boolean> resetPassword(ResetPasswordDto resetPasswordDto) {
        try {
            validatePasswords(resetPasswordDto.getPassword(), resetPasswordDto.getConfirmPassword());

            Optional<UserRecord> user = userRepository.findByResetPasswordToken(resetPasswordDto.getToken());
            if (user.isEmpty()) {
                return logAndReturnError("Invalid or expired reset token.", "invalid_token");
            }

            UserRecord userRecord = user.get();
            userRecord.setPassword(passwordEncoder.encode(resetPasswordDto.getPassword()));
            userRecord.setResetPasswordToken(null);
            userRepository.save(userRecord);

            return ServiceResponse.value(true);
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while resetting password.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while resetting password.", "unexpected_error", e);
        }
    }

    public ServiceResponse<Boolean> forgotPassword(String username) {
        try {
            Optional<UserRecord> user = userRepository.findByUsername(username);
            if (user.isEmpty()) {
                return logAndReturnError("User not found with username " + username, "user_not_found");
            }

            Optional<UserProfile> userProfile = userProfileRepository.findById(user.get().getUserId());
            if (userProfile.isEmpty()) {
                return logAndReturnError("UserProfile not found for userId: " + user.get().getUserId(), "user_profile_not_found");
            }

            String token = UUID.randomUUID().toString();
            UserRecord userRecord = user.get();
            userRecord.setResetPasswordToken(token);
            userRepository.save(userRecord);

            emailService.sendResetPasswordEmailAsync(userProfile.get().getEmail(), "https://fremontmi.com/reset-password?token=" + token);
            return ServiceResponse.value(true);
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while processing forgot password.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while processing forgot password.", "unexpected_error", e);
        }
    }

    public ServiceResponse<Boolean> forgotUsername(String email) {
        try {
            Optional<UserProfile> userProfile = userProfileRepository.findByEmail(email);
            if (userProfile.isEmpty()) {
                return logAndReturnError("User profile not found with email " + email, "email_not_found");
            }

            Optional<UserRecord> user = userRepository.findById(userProfile.get().getUserId());
            if (user.isEmpty()) {
                return logAndReturnError("UserRecord not found for userId: " + userProfile.get().getUserId(), "user_record_not_found");
            }

            emailService.sendForgotUsernameEmailAsync(userProfile.get().getEmail(), user.get().getUsername());
            return ServiceResponse.value(true);
        } catch (DataAccessException e) {
            return logAndReturnError("Database error while processing forgot username.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error while processing forgot username.", "unexpected_error", e);
        }
    }

    private void validatePasswords(String password, String matchingPassword) throws ValidationException {
        if (password == null || !password.equals(matchingPassword)) {
            throw new ValidationException("Passwords do not match.");
        }
    }

    private UserRecord createUserRecord(UserRegistrationDto dto) {
        UserRecord userRecord = new UserRecord();
        userRecord.setUsername(dto.getUsername());
        userRecord.setPassword(passwordEncoder.encode(dto.getPassword()));
        return userRecord;
    }

    private UserProfile createUserProfile(UserRegistrationDto dto, String userId) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setEmail(dto.getEmail());
        profile.setFirstName(dto.getFirstName());
        profile.setLastName(dto.getLastName());
        profile.setTermsAcceptedAt(dto.getTermsAcceptedAt());
        return profile;
    }

    // TODO not good, this did not save all the values I wanted it to and the abstraction should be removed
    private void updateProfileDetails(UserProfile existingProfile, UserProfile updatedProfile) {
        existingProfile.setFirstName(updatedProfile.getFirstName());
        existingProfile.setLastName(updatedProfile.getLastName());
        existingProfile.setEmail(updatedProfile.getEmail());
    }

    private <T> ServiceResponse<T> logAndReturnError(String message, String errorCode) {
        logger.error(message);
        return ServiceResponse.error(errorCode);
    }

    private <T> ServiceResponse<T> logAndReturnError(String message, String errorCode, Exception e) {
        logger.error(message, e);
        return ServiceResponse.error(errorCode);
    }

    public List<UserProfile> getUserProfiles(List<String> members) {
        return userProfileRepository.findAllById(members);
    }
}
