package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BusinessService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessService.class);

    private final ContentRepository contentRepository;

    private final UserService userService;

    private final StripeService stripeService;

    public BusinessService(ContentRepository contentRepository, UserService userService, StripeService stripeService) {
        this.contentRepository = contentRepository;
        this.userService = userService;
        this.stripeService = stripeService;
    }

    public ServiceResponse<Business> createBusiness(Business business) {
        Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

        if (optionalUserProfile.isEmpty()) {
            return logAndReturnError("Failed to create business: user profile not found.", "user_profile_not_found");
        }

        UserProfile userProfile = optionalUserProfile.get();

        business.setType(ContentTypes.BUSINESS.getContentType());
        business.setSlug(createUniqueSlug(business.getName()));
        business.setPathname("/businesses/" + business.getSlug());
        business.setVisibility(ContentVisibility.RESTRICTED.getVisibility());
        business.setStatus(ContentStatus.REQUIRES_ACTIVE_SUBSCRIPTION.getStatus());
        business.setCreatedBy(userProfile.getUserId());

        ServiceResponse<Map<String, Object>> createSubscriptionResponse = stripeService.createSubscription(business.getPriceId());

        if (createSubscriptionResponse.hasError()) {
            logger.error("Error when trying to create a subscription");
            return ServiceResponse.error(createSubscriptionResponse.errorCode());
        }

        Map<String, Object> stripeDetails = createSubscriptionResponse.value();

        business.setStripeDetails(stripeDetails);

        ServiceResponse<Business> saveResponse = saveBusiness(business);

        if (saveResponse.hasError()) {
            logger.error("Error when trying to save a Businesss");
            return ServiceResponse.error(saveResponse.errorCode());
        }

        Business savedBusiness = saveResponse.value();

        userProfile.getBusinessIds().add(savedBusiness.getId());

        ServiceResponse<UserProfile> saveUserProfileResponse = userService.saveUserProfile(userProfile);

        if (saveUserProfileResponse.hasError()) {
            logger.error("Error when trying to save a UserProfile");
            return ServiceResponse.error(saveUserProfileResponse.errorCode());
        }

        return ServiceResponse.value(savedBusiness);
    }

    public ServiceResponse<Business> saveBusiness(Business business) {
        try {
            return ServiceResponse.value(contentRepository.save(business));
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to create a business", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to create a business", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    private Boolean hasPermission(String userId, Business business) {
        return business.getAdministrators().contains(userId);
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

    public ServiceResponse<List<Business>> findAllBusinesses() {
        try {
            List<Content> contents = contentRepository.findAllByType(ContentTypes.BUSINESS.getContentType());

            List<Business> businesses = contents.stream()
                    .filter(content -> content instanceof Business)
                    .map(content -> (Business) content)
                    .collect(Collectors.toList());

            return ServiceResponse.value(businesses);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to find active businesses", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to find active businesses", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public Optional<Business> findBusinessById(String businessId) {
        try {
            Optional<Content> contentOpt = contentRepository.findById(businessId);

            return contentOpt
                    .filter(content -> content instanceof Business)
                    .map(content -> (Business) content);

        } catch (DataAccessException e) {
            logger.error("Database access error when trying to find a business by id", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error when trying to find a business by id", e);
            return Optional.empty();
        }
    }

    public Optional<Business> findBusinessBySlug(String slug) {
        try {
            Optional<Content> contentOpt = contentRepository.findBySlug(slug);

            return contentOpt
                    .filter(content -> content instanceof Business)
                    .map(content -> (Business) content);

        } catch (DataAccessException e) {
            logger.error("Database access error when trying to find a business by id", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error when trying to find a business by id", e);
            return Optional.empty();
        }
    }

    public ServiceResponse<List<Business>> findBusinessesByUser () {
        Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

        if (optionalUserProfile.isEmpty()) {
            return ServiceResponse.error("user_profile_not_found");
        }

        UserProfile userProfile = optionalUserProfile.get();

        try {
            List<Content> contents = contentRepository.findAllById(userProfile.getBusinessIds());

            List<Business> businesses = contents.stream()
                    .filter(content -> content instanceof Business)
                    .map(content -> (Business) content)
                    .collect(Collectors.toList());

            return ServiceResponse.value(businesses);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to retrieve all businesses for the user", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to retrieve all businesses for the user", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public ServiceResponse<Business> updateBusiness(Business business) {
        try {
            Optional<String> optionalUserId = userService.getUserId();

            if (optionalUserId.isEmpty()) {
                return logAndReturnError("Failed to delete neighbor service: user id not found.", "user_id_not_found");
            }

            String userId = optionalUserId.get();

            if (!hasPermission(userId, business)) {
                return logAndReturnError("User does not have permission to update this business", "permission_denied");
            }

            // actually update the properties

            ServiceResponse<Business> savedBusinessResponse = saveBusiness(business);

            if (savedBusinessResponse.hasError()) {
                return ServiceResponse.error(savedBusinessResponse.errorCode());
            }

            Business savedBusiness = savedBusinessResponse.value();

            return ServiceResponse.value(savedBusiness);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to update a business", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to update a business", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public ServiceResponse<Boolean> deleteBusiness(String businessId) {
        Optional<Business> optionalBusiness = findBusinessById(businessId);

        if (optionalBusiness.isEmpty()) {
            return ServiceResponse.error("business_not_found");
        }

        Business business = optionalBusiness.get();

        if (business.getSubscriptionId() != null && !business.getSubscriptionId().isEmpty()) {
            ServiceResponse<Boolean> isSubscriptionActiveResponse = stripeService.isSubscriptionActive(business.getSubscriptionId());

            if (isSubscriptionActiveResponse.hasError()) {
                return ServiceResponse.error(isSubscriptionActiveResponse.errorCode());
            }

            if (isSubscriptionActiveResponse.value()) {
                return ServiceResponse.error("subscription_still_active");
            }
        }

        try {
            contentRepository.deleteById(businessId);
            return ServiceResponse.value(true);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to delete a business", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to delete a business", e);
            return ServiceResponse.error("unexpected_error");
        }
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
