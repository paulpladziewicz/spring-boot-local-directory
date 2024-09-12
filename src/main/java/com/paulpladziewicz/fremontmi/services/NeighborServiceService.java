package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.NeighborServiceProfileRepository;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NeighborServiceService {
    private static final Logger logger = LoggerFactory.getLogger(NeighborServiceService.class);

    private final NeighborServiceProfileRepository neighborServiceProfileRepository;

    private final UserService userService;

    private final StripeService stripeService;

    public NeighborServiceService(NeighborServiceProfileRepository neighborServiceProfileRepository, UserService userService, StripeService stripeService) {
        this.neighborServiceProfileRepository = neighborServiceProfileRepository;
        this.userService = userService;
        this.stripeService = stripeService;
    }

    public ServiceResponse<StripeTransactionRecord> createNeighborServiceProfile(String priceId, NeighborServiceProfile neighborServiceProfile) {
        Optional<String> optionalUserId = userService.getUserId();

        if (optionalUserId.isEmpty()) {
            return ServiceResponse.error("user_id_not_found");
        }

        String userId = optionalUserId.get();

        neighborServiceProfile.setId(userId);

        ServiceResponse<StripeTransactionRecord> createSubscriptionResponse = stripeService.createSubscription(priceId);

        if (createSubscriptionResponse.hasError()) {
            logger.error("Error when trying to create a subscription");
            return ServiceResponse.error(createSubscriptionResponse.errorCode());
        }

        StripeTransactionRecord stripeTransactionRecord = createSubscriptionResponse.value();

        ServiceResponse<NeighborServiceProfile> savedNeighborServiceProfileResponse = saveNeighborServiceProfile(neighborServiceProfile);

        if (savedNeighborServiceProfileResponse.hasError()) {
            logger.error("Error when trying to save a NeighborServiceProfile");
            return ServiceResponse.error(savedNeighborServiceProfileResponse.errorCode());
        }

        return ServiceResponse.value(stripeTransactionRecord);
    }

    public ServiceResponse<NeighborServiceProfile> saveNeighborServiceProfile(NeighborServiceProfile neighborServiceProfile) {
        try {
            NeighborServiceProfile savedNeighborServiceProfile = neighborServiceProfileRepository.save(neighborServiceProfile);

            return ServiceResponse.value(savedNeighborServiceProfile);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to save a NeighborServiceProfile", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to save a NeighborServiceProfile", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public ServiceResponse<NeighborServiceProfile> handleSubscriptionSuccess(PaymentRequest paymentRequest) {
        String neighborServiceId = paymentRequest.getId();
        String paymentIntentId = paymentRequest.getPaymentIntentId();
        String paymentStatus = paymentRequest.getPaymentStatus();

        if (neighborServiceId == null || neighborServiceId.isEmpty() || paymentIntentId == null || paymentIntentId.isEmpty() || paymentStatus == null || paymentStatus.isEmpty()) {
            return ServiceResponse.error("required_info_not_correct");
        }

        Optional<NeighborServiceProfile> optionalNeighborServiceProfile = neighborServiceProfileRepository.findById(neighborServiceId);

        if (optionalNeighborServiceProfile.isEmpty()) {
            return ServiceResponse.error("neighbor_service_not_found");
        }

        NeighborServiceProfile neighborServiceProfile = optionalNeighborServiceProfile.get();

        ServiceResponse<PaymentIntent> serviceResponse = stripeService.retrievePaymentIntent(paymentIntentId);

        if (serviceResponse.hasError()) {
            return ServiceResponse.error(serviceResponse.errorCode());
        }

        PaymentIntent paymentIntent = serviceResponse.value();

        if (paymentIntent.getStatus().equals("succeeded")) {
            neighborServiceProfile.setStatus("active");
        } else {
            return ServiceResponse.error("payment_not_successful");
        }

        ServiceResponse<NeighborServiceProfile> savedNeighborServiceProfileResponse = saveNeighborServiceProfile(neighborServiceProfile);

        if (savedNeighborServiceProfileResponse.hasError()) {
            return ServiceResponse.error(savedNeighborServiceProfileResponse.errorCode());
        }

        return ServiceResponse.value(neighborServiceProfile);
    }

    public ServiceResponse<List<NeighborServiceProfile>> findAllActiveNeighborServices() {
        try {
            List<NeighborServiceProfile> activeNeighborServiceProfiles = neighborServiceProfileRepository.findByStatus("active");
            return ServiceResponse.value(activeNeighborServiceProfiles);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to find active neighbor services", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to find active neighbor services", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public Optional<NeighborServiceProfile> findNeighborServiceProfileById(String neighborServiceProfileId) {
        try {
            return neighborServiceProfileRepository.findById(neighborServiceProfileId);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to find a neighbor service by id", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error when trying to find a neighbor service by id", e);
            return Optional.empty();
        }
    }

    public ServiceResponse<Boolean> deleteNeighborService(String neighborServiceId) {
        Optional<NeighborServiceProfile> optionalNeighborServiceProfile = findNeighborServiceProfileById(neighborServiceId);

        if (optionalNeighborServiceProfile.isEmpty()) {
            return ServiceResponse.error("neighbor_service_not_found");
        }

        NeighborServiceProfile neighborServiceProfile = optionalNeighborServiceProfile.get();

        // TODO check if Stripe subscription is still active and cancel it

        try {
            neighborServiceProfileRepository.deleteById(neighborServiceId);
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
