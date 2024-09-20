package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NeighborServicesProfileService {
    private static final Logger logger = LoggerFactory.getLogger(NeighborServicesProfileService.class);

    private final ContentRepository contentRepository;

    private final UserService userService;

    private final StripeService stripeService;

    public NeighborServicesProfileService(ContentRepository contentRepository, UserService userService, StripeService stripeService) {
        this.contentRepository = contentRepository;
        this.userService = userService;
        this.stripeService = stripeService;
    }

    public ServiceResponse<NeighborServicesProfile> createNeighborServiceProfile(NeighborServicesProfile neighborServicesProfile) {
        Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

        if (optionalUserProfile.isEmpty()) {
            return logAndReturnError("Failed to create neighbor service profile: user profile not found.", "user_profile_not_found");
        }

        UserProfile userProfile = optionalUserProfile.get();

        neighborServicesProfile.setType(ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());
        neighborServicesProfile.setSlug(createUniqueSlug(neighborServicesProfile.getName()));
        neighborServicesProfile.setPathname("/neighbor-services/" + neighborServicesProfile.getSlug());
        neighborServicesProfile.setVisibility(ContentVisibility.RESTRICTED.getVisibility());
        neighborServicesProfile.setStatus(ContentStatus.REQUIRES_ACTIVE_SUBSCRIPTION.getStatus());
        neighborServicesProfile.setCreatedBy(userProfile.getUserId());

        ServiceResponse<Map<String, Object>> createSubscriptionResponse = stripeService.createSubscription(neighborServicesProfile.getPriceId());

        if (createSubscriptionResponse.hasError()) {
            logger.error("Error when trying to create a subscription");
            return ServiceResponse.error(createSubscriptionResponse.errorCode());
        }

        Map<String, Object> stripeDetails = createSubscriptionResponse.value();

        neighborServicesProfile.setStripeDetails(stripeDetails);

        ServiceResponse<NeighborServicesProfile> saveResponse = saveNeighborServiceProfile(neighborServicesProfile);

        if (saveResponse.hasError()) {
            logger.error("Error when trying to save a NeighborServicesProfile");
            return ServiceResponse.error(saveResponse.errorCode());
        }

        NeighborServicesProfile savedNeighborServicesProfile = saveResponse.value();


        return ServiceResponse.value(savedNeighborServicesProfile);
    }

    // TODO make this more efficient
    public String createUniqueSlug(String name) {
        // Clean up the name to form the base slug
        String baseSlug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");

        // Find all slugs that start with the base slug
        List<Content> matchingSlugs = contentRepository.findBySlugRegex("^" + baseSlug + "(-\\d+)?$");

        // If no matching slugs, return the base slug
        if (matchingSlugs.isEmpty()) {
            return baseSlug;
        }

        // Extract slugs that match the baseSlug-<number> format
        Pattern pattern = Pattern.compile(Pattern.quote(baseSlug) + "-(\\d+)$");

        int maxNumber = 0;
        boolean baseSlugExists = false;

        for (Content content : matchingSlugs) {
            String slug = content.getSlug();

            // Check if the base slug without a number already exists
            if (slug.equals(baseSlug)) {
                baseSlugExists = true;
            }

            // Find the slugs with numbers at the end and get the max number
            Matcher matcher = pattern.matcher(slug);
            if (matcher.find()) {
                int number = Integer.parseInt(matcher.group(1));
                maxNumber = Math.max(maxNumber, number);
            }
        }

        // If the base slug already exists, start numbering from 1
        if (baseSlugExists) {
            return baseSlug + "-" + (maxNumber + 1);
        } else if (maxNumber > 0) {
            return baseSlug + "-" + (maxNumber + 1);
        } else {
            return baseSlug;  // No suffix needed if base slug doesn't exist
        }
    }

    public ServiceResponse<NeighborServicesProfile> saveNeighborServiceProfile(NeighborServicesProfile neighborServicesProfile) {
        try {
            NeighborServicesProfile savedNeighborServicesProfile = contentRepository.save(neighborServicesProfile);
            return ServiceResponse.value(savedNeighborServicesProfile);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to save a NeighborServicesProfile", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to save a NeighborServicesProfile", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public ServiceResponse<List<NeighborServicesProfile>> findAllActiveNeighborServices(String tag) {
        try {
            List<Content> contents;

            if (tag != null && !tag.isEmpty()) {
                contents = contentRepository.findByTagAndType(tag, ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());
            } else {
                contents = contentRepository.findAllByType(ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());
            }

            List<NeighborServicesProfile> profiles = contents.stream()
                    .filter(content -> content instanceof NeighborServicesProfile) // Ensure type safety
                    .map(content -> (NeighborServicesProfile) content)
                    .collect(Collectors.toList());

            return ServiceResponse.value(profiles);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to find active neighbor services", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to find active neighbor services", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public Optional<NeighborServicesProfile> findNeighborServiceProfileById(String neighborServiceProfileId) {
        try {
            Optional<Content> optionalContent = contentRepository.findById(neighborServiceProfileId);

            return optionalContent
                    .filter(content -> content instanceof NeighborServicesProfile)
                    .map(content -> (NeighborServicesProfile) content);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to find a neighbor service by id", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error when trying to find a neighbor service by id", e);
            return Optional.empty();
        }
    }

    public Optional<NeighborServicesProfile> findNeighborServiceProfileBySlug(String neighborServiceProfileSlug) {
        try {
            Optional<Content> optionalContent = contentRepository.findBySlug(neighborServiceProfileSlug);

            return optionalContent
                    .filter(content -> content instanceof NeighborServicesProfile)
                    .map(content -> (NeighborServicesProfile) content);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to find a neighbor service by id", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error when trying to find a neighbor service by id", e);
            return Optional.empty();
        }
    }

    public Optional<NeighborServicesProfile> findNeighborServiceProfileByUserId() {
        Optional<String> optionalUserId = userService.getUserId();

        if (optionalUserId.isEmpty()) {
            return Optional.empty();
        }

        String userId = optionalUserId.get();

        try {
            Optional<Content> optionalContent = contentRepository.findByCreatedBy(userId);

            return optionalContent
                    .filter(content -> content instanceof NeighborServicesProfile)
                    .map(content -> (NeighborServicesProfile) content);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to find a neighbor service by user id", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error when trying to find a neighbor service by user id", e);
            return Optional.empty();
        }
    }

    public ServiceResponse<NeighborServicesProfile> updateNeighborServiceProfile(NeighborServicesProfile neighborServiceProfile) {
        Optional<String> optionalUserId = userService.getUserId();

        if (optionalUserId.isEmpty()) {
            return logAndReturnError("Failed to delete neighbor service: user id not found.", "user_id_not_found");
        }

        String userId = optionalUserId.get();

        Optional<NeighborServicesProfile> optionalNeighborServiceProfile = findNeighborServiceProfileById(neighborServiceProfile.getId());

        if (optionalNeighborServiceProfile.isEmpty()) {
            return ServiceResponse.error("neighbor_service_not_found");
        }

        NeighborServicesProfile existingProfile = optionalNeighborServiceProfile.get();

        if (!hasPermission(userId, existingProfile)) {
            return ServiceResponse.error("permission_denied");
        }

        updateExistingNeighborServiceProfile(existingProfile, neighborServiceProfile);

        ServiceResponse<NeighborServicesProfile> saveResponse = saveNeighborServiceProfile(existingProfile);

        if (saveResponse.hasError()) {
            return ServiceResponse.error(saveResponse.errorCode());
        }

        return ServiceResponse.value(saveResponse.value());
    }

    public ServiceResponse<Boolean> deleteNeighborService(String neighborServiceId) {
        Optional<String> optionalUserId = userService.getUserId();

        if (optionalUserId.isEmpty()) {
            return logAndReturnError("Failed to delete neighbor service: user id not found.", "user_id_not_found");
        }

        String userId = optionalUserId.get();

        Optional<NeighborServicesProfile> optionalNeighborServiceProfile = findNeighborServiceProfileById(neighborServiceId);

        if (optionalNeighborServiceProfile.isEmpty()) {
            return ServiceResponse.error("neighbor_service_not_found");
        }

        NeighborServicesProfile neighborServicesProfile = optionalNeighborServiceProfile.get();

        if (!hasPermission(userId, neighborServicesProfile)) {
            return ServiceResponse.error("permission_denied");
        }

        // TODO check if Stripe subscription is still active and cancel it

        try {
            contentRepository.deleteById(neighborServiceId);
            return ServiceResponse.value(true);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to delete a business", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to delete a business", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    private Boolean hasPermission(String userId, NeighborServicesProfile neighborServicesProfile) {
        return neighborServicesProfile.getCreatedBy().equals(userId);
    }

    private void updateExistingNeighborServiceProfile(NeighborServicesProfile existingProfile, NeighborServicesProfile updatedProfile) {
        // Update basic fields
        existingProfile.setName(updatedProfile.getName());
        existingProfile.setDescription(updatedProfile.getDescription());
        existingProfile.setEmail(updatedProfile.getEmail());
        existingProfile.setTags(updatedProfile.getTags());

        // Update neighbor services if provided
        if (updatedProfile.getNeighborServices() != null) {
            existingProfile.setNeighborServices(updatedProfile.getNeighborServices());
        }

        // Set updatedAt to the current time
        existingProfile.setUpdatedAt(LocalDateTime.now());
    }


    private <T> ServiceResponse<T> logAndReturnError(String message, String errorCode) {
        logger.error(message);
        return ServiceResponse.error(errorCode);
    }

    private <T> ServiceResponse<T> logAndReturnError(String message, String errorCode, Exception e) {
        logger.error(message, e);
        return ServiceResponse.error(errorCode);
    }
}
