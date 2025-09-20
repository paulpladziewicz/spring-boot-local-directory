package com.paulpladziewicz.fremontmi.user;

import com.paulpladziewicz.fremontmi.app.ServiceResponse;
import com.paulpladziewicz.fremontmi.app.exceptions.UserNotAuthenticatedException;
import com.paulpladziewicz.fremontmi.app.exceptions.UserProfileNotFoundException;
import com.paulpladziewicz.fremontmi.notification.EmailService;

import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
        if (userRepository.existsByUsername(userRegistrationDto.getUsername())) {
            return logAndReturnError("Username already exists.", "username_exists");
        }

        if (userProfileRepository.existsByEmail(userRegistrationDto.getEmail())) {
            return logAndReturnError("Email already exists.", "email_exists");
        }

        try {
            validatePasswords(userRegistrationDto.getPassword(), userRegistrationDto.getMatchingPassword());
        } catch (ValidationException e) {
            return ServiceResponse.error("password_invalid");
        }

        UserRecord newUserRecord = createUserRecord(userRegistrationDto);
        UserRecord savedUser = userRepository.save(newUserRecord);

        UserProfile savedUserProfile = createUserProfile(userRegistrationDto, savedUser.getUserId());
        userProfileRepository.save(savedUserProfile);

        emailService.sendWelcomeEmailAsync(userRegistrationDto.getEmail(), savedUser.getConfirmationToken());

        logger.info("Successfully created user {} with email {}", savedUser.getUsername(), savedUserProfile.getEmail());
        return ServiceResponse.value(true);
    }

    public String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            CustomUserDetails customUserDetails = (CustomUserDetails) auth.getPrincipal();
            return customUserDetails.getUserId();
        }

        throw new UserNotAuthenticatedException("User is not authenticated.");
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            return authorities.stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        }

        throw new UserNotAuthenticatedException("User is not authenticated.");
    }

    public UserProfile getUserProfile() {
        String userId = getUserId();
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException("User profile for userId '" + userId + "' could not be retrieved."));
    }

    public void saveUserProfile(UserProfile userProfile) {
        userProfileRepository.save(userProfile);
    }

    public ServiceResponse<UserProfile> updateUserProfile(UserProfile updatedUserProfile) {
        UserProfile userProfile = getUserProfile();
        updateProfileDetails(userProfile, updatedUserProfile);

        return ServiceResponse.value(userProfileRepository.save(userProfile));
    }

    public ServiceResponse<Boolean> resetPassword(ResetPasswordDto resetPasswordDto) {
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
    }

    public ServiceResponse<Boolean> forgotPassword(String username) {
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
    }

    public ServiceResponse<Boolean> forgotUsername(String email) {
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
        userRecord.setConfirmationToken(UUID.randomUUID().toString());
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

    public List<UserProfile> getUserProfiles(Set<String> members) {
        return userProfileRepository.findAllById(members);
    }

    public ServiceResponse<Boolean> confirmUser(String token) {
        UserRecord user = userRepository.findByConfirmationToken(token);
        if (user != null) {
            user.setEnabled(true);
            user.setConfirmationToken(null);
            userRepository.save(user);
            return ServiceResponse.value(true);
        } else {
            return ServiceResponse.error("invalid_token");
        }
    }
}
