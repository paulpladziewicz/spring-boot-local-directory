package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.NeighborServiceRepository;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NeighborServiceService {
    private static final Logger logger = LoggerFactory.getLogger(NeighborServiceService.class);

    private final NeighborServiceRepository neighborServiceRepository;

    private final UserService userService;
    private final StripeService stripeService;

    public NeighborServiceService(NeighborServiceRepository neighborServiceRepository, UserService userService, StripeService stripeService) {
        this.neighborServiceRepository = neighborServiceRepository;
        this.userService = userService;
        this.stripeService = stripeService;
    }

    public ServiceResponse<NeighborService> createNeighborService(NeighborService neighborService) {
        ServiceResponse<Map<String, Object>> serviceResponse = stripeService.createSubscription(neighborService.getSubscriptionPriceId());

        if (serviceResponse.hasError()) {
            return ServiceResponse.error(serviceResponse.errorCode());
        }

        neighborService.setClientSecret((String) serviceResponse.value().get("clientSecret"));
        neighborService.setStripeSubscriptionId((String) serviceResponse.value().get("subscriptionId"));

        try {
            Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

            if (optionalUserProfile.isEmpty()) {
                return ServiceResponse.error("user_profile_not_found");
            }

            UserProfile userProfile = optionalUserProfile.get();

            NeighborService savedNeighborService = neighborServiceRepository.save(neighborService);

            userProfile.getNeighborServiceIds().add(savedNeighborService.getId());

            ServiceResponse<UserProfile> saveUserProfileResponse =  userService.saveUserProfile(userProfile);

            if (saveUserProfileResponse.hasError()) {
                return ServiceResponse.error(saveUserProfileResponse.errorCode());
            }

            return ServiceResponse.value(savedNeighborService);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to create a NeighborService", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to create a NeighborService", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public ServiceResponse<NeighborService> handleSubscriptionSuccess(PaymentRequest paymentRequest) {
        String neighborServiceId = paymentRequest.getId();
        String paymentIntentId = paymentRequest.getPaymentIntentId();
        String paymentStatus = paymentRequest.getPaymentStatus();

        if (neighborServiceId == null || neighborServiceId.isEmpty() || paymentIntentId == null || paymentIntentId.isEmpty() || paymentStatus == null || paymentStatus.isEmpty()) {
            return ServiceResponse.error("required_info_not_correct");
        }

        Optional<NeighborService> optionalNeighborService = neighborServiceRepository.findById(neighborServiceId);

        if (optionalNeighborService.isEmpty()) {
            return ServiceResponse.error("neighbor_service_not_found");
        }

        NeighborService neighborService = optionalNeighborService.get();

        ServiceResponse<PaymentIntent> serviceResponse = stripeService.retrievePaymentIntent(paymentIntentId);
        if (serviceResponse.hasError()) {
            return ServiceResponse.error(serviceResponse.errorCode());
        }

        PaymentIntent paymentIntent = serviceResponse.value();

        neighborService.setPaymentIntentId(paymentIntent.getId());
        neighborService.setPaymentStatus(paymentIntent.getStatus());

        if (paymentIntent.getStatus().equals("succeeded")) {
            neighborService.setStatus("active");
        } else {
            neighborService.setStatus("inactive");
        }

        try {
            return ServiceResponse.value(neighborServiceRepository.save(neighborService));
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to create a neighbor service", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to create a neighbor service", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public ServiceResponse<List<NeighborService>> findAllActiveNeighborServices() {
        try {
            List<NeighborService> activeNeighborServices = neighborServiceRepository.findByStatus("active");
            return ServiceResponse.value(activeNeighborServices);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to find active neighbor services", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to find active neighbor services", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public Optional<NeighborService> findNeighborServiceById(String neighborServiceId) {
        try {
            return neighborServiceRepository.findById(neighborServiceId);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to find a neighbor service by id", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error when trying to find a neighbor service by id", e);
            return Optional.empty();
        }
    }

    public ServiceResponse<List<NeighborService>> findNeighborServicesByUser () {
        Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

        if (optionalUserProfile.isEmpty()) {
            return ServiceResponse.error("user_profile_not_found");
        }

        UserProfile userProfile = optionalUserProfile.get();

        try {
            return ServiceResponse.value(neighborServiceRepository.findAllById(userProfile.getNeighborServiceIds()));
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to retrieve all neighbor services for the user", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to retrieve all neighbor services for the user", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public ServiceResponse<NeighborService> updateNeighborService(NeighborService neighborService) {
        try {
            return ServiceResponse.value(neighborServiceRepository.save(neighborService));
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to update a business", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to update a business", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public ServiceResponse<Boolean> deleteNeighborService(String neighborServiceId) {
        Optional<NeighborService> optionalNeighborService = findNeighborServiceById(neighborServiceId);

        if (optionalNeighborService.isEmpty()) {
            return ServiceResponse.error("neighbor_service_not_found");
        }

        NeighborService neighborService = optionalNeighborService.get();

        if (neighborService.getStripeSubscriptionId() != null && !neighborService.getStripeSubscriptionId().isEmpty()) {
            ServiceResponse<Boolean> isSubscriptionActiveResponse = stripeService.isSubscriptionActive(neighborService.getStripeSubscriptionId());

            if (isSubscriptionActiveResponse.hasError()) {
                return ServiceResponse.error(isSubscriptionActiveResponse.errorCode());
            }

            if (isSubscriptionActiveResponse.value()) {
                return ServiceResponse.error("subscription_still_active");
            }
        }

        try {
            neighborServiceRepository.deleteById(neighborServiceId);
            return ServiceResponse.value(true);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to delete a business", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to delete a business", e);
            return ServiceResponse.error("unexpected_error");
        }
    }
}
