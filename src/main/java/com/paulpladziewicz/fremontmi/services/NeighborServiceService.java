package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.NeighborServiceProfileRepository;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NeighborServiceService {
    private static final Logger logger = LoggerFactory.getLogger(NeighborServiceService.class);

    private final MongoTemplate mongoTemplate;

    private final NeighborServiceProfileRepository neighborServiceProfileRepository;

    private final UserService userService;

    private final StripeService stripeService;

    public NeighborServiceService(MongoTemplate mongoTemplate, NeighborServiceProfileRepository neighborServiceProfileRepository, UserService userService, StripeService stripeService) {
        this.mongoTemplate = mongoTemplate;
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

        neighborServiceProfile.setUserId(userId);

        ServiceResponse<NeighborServiceProfile> savedNeighborServiceProfileResponse = saveNeighborServiceProfile(neighborServiceProfile);

        if (savedNeighborServiceProfileResponse.hasError()) {
            logger.error("Error when trying to save a NeighborServiceProfile");
            return ServiceResponse.error(savedNeighborServiceProfileResponse.errorCode());
        }

        NeighborServiceProfile savedNeighborServiceProfile = savedNeighborServiceProfileResponse.value();

        ServiceResponse<StripeTransactionRecord> createSubscriptionResponse = stripeService.createSubscription(priceId, savedNeighborServiceProfile.getId(), "neighbor_services_profiles");

        if (createSubscriptionResponse.hasError()) {
            logger.error("Error when trying to create a subscription");
            return ServiceResponse.error(createSubscriptionResponse.errorCode());
        }

        StripeTransactionRecord stripeTransactionRecord = createSubscriptionResponse.value();



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

    public ServiceResponse<List<NeighborServiceProfile>> findAllActiveNeighborServices(String tag) {
        try {
            List<NeighborServiceProfile> profiles;

            if (tag != null && !tag.isEmpty()) {
                profiles = neighborServiceProfileRepository.findByTagsContainingAndStatus(tag, "active");
            } else {
                profiles = neighborServiceProfileRepository.findByStatus("active");
            }

            return ServiceResponse.value(profiles);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to find active neighbor services", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to find active neighbor services", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    // Fetch all distinct tags by querying for documents and manually processing the tags field
    public List<String> findAllDistinctTags() {
        // Query to find all documents where "tags" exists and is not an empty array
        Query query = new Query();
        query.addCriteria(Criteria.where("tags").exists(true).not().size(0));

        // Find documents with non-empty tags
        List<NeighborServiceProfile> profiles = mongoTemplate.find(query, NeighborServiceProfile.class, "neighbor_services_profiles");

        // Use a Set to automatically handle uniqueness
        Set<String> distinctTags = new HashSet<>();

        // Loop through all profiles and collect tags
        for (NeighborServiceProfile profile : profiles) {
            if (profile.getTags() != null) {
                for (String tag : profile.getTags()) {
                    if (tag != null && !tag.trim().isEmpty()) {
                        distinctTags.add(tag);  // Add only non-empty, non-null tags
                    }
                }
            }
        }

        // Convert the set of distinct tags to a list
        return new ArrayList<>(distinctTags);
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

    public Optional<NeighborServiceProfile> findNeighborServiceProfileByUserId() {
        Optional<String> optionalUserId = userService.getUserId();

        if (optionalUserId.isEmpty()) {
            return Optional.empty();
        }

        String userId = optionalUserId.get();

        try {
            return neighborServiceProfileRepository.findByUserId(userId);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to find a neighbor service by user id", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error when trying to find a neighbor service by user id", e);
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
