package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.NeighborServiceProfileRepository;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
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
        String neighborServiceProfileId = paymentRequest.getEntityId();
        String paymentIntentId = paymentRequest.getPaymentIntentId();
        String paymentStatus = paymentRequest.getPaymentStatus();

        if (!paymentStatus.equals("succeeded")) {
            logger.error("Payment not successful but it should have been when handling successful payment. Payment status: {} neighborServiceProfileId: {}", paymentStatus, neighborServiceProfileId);
            return ServiceResponse.error("payment_not_successful");
        }

        Optional<NeighborServiceProfile> optionalNeighborServiceProfile = neighborServiceProfileRepository.findById(neighborServiceProfileId);

        if (optionalNeighborServiceProfile.isEmpty()) {
            return ServiceResponse.error("neighbor_service_not_found");
        }

        NeighborServiceProfile neighborServiceProfile = optionalNeighborServiceProfile.get();

        if (!neighborServiceProfile.getStatus().equals("active")) {
            neighborServiceProfile.setStatus("active");

            ServiceResponse<NeighborServiceProfile> savedNeighborServiceProfileResponse = saveNeighborServiceProfile(neighborServiceProfile);

            if (savedNeighborServiceProfileResponse.hasError()) {
                return ServiceResponse.error(savedNeighborServiceProfileResponse.errorCode());
            }
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

    // Example Aggregation query to find the top 'n' tags used in NeighborServiceProfiles
    public List<TagUsage> getTopTags(int limit) {
        // Step 1: Unwind the "tags" array
        AggregationOperation unwindTags = Aggregation.unwind("tags");

        // Step 2: Group by tag and count occurrences
        GroupOperation groupByTag = Aggregation.group("tags").count().as("count");

        // Step 3: Sort by count in descending order
        AggregationOperation sortByCount = Aggregation.sort(Sort.by(Sort.Direction.DESC, "count"));

        // Step 4: Limit the results to the top 'n' tags
        AggregationOperation limitResults = Aggregation.limit(limit);

        // Step 5: Combine the aggregation steps into a pipeline
        Aggregation aggregation = Aggregation.newAggregation(unwindTags, groupByTag, sortByCount, limitResults);

        // Step 6: Execute the aggregation query using MongoTemplate
        AggregationResults<TagUsage> results = mongoTemplate.aggregate(aggregation, "neighbor_services_profiles", TagUsage.class);

        // Return the list of top tags
        return results.getMappedResults();
    }

    public ServiceResponse<List<NeighborServiceProfile>> searchNeighborServiceProfiles(String query) {
        try {
            // Define the search aggregation using MongoDB Atlas Search
            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("$search").is("pets")),
                    Aggregation.match(new Criteria().orOperator(
                            Criteria.where("firstName").regex(query, "i"),
                            Criteria.where("lastName").regex(query, "i"),
                            Criteria.where("description").regex(query, "i"),
                            Criteria.where("tags").regex(query, "i")
                    ))
            );

            // Execute the aggregation query
            AggregationResults<NeighborServiceProfile> results = mongoTemplate.aggregate(aggregation, "neighbor_services_profiles", NeighborServiceProfile.class);

            // Return the search results
            return ServiceResponse.value(results.getMappedResults());
        } catch (Exception e) {
            logger.error("Error while searching for NeighborServiceProfiles", e);
            return ServiceResponse.error("search_failed");
        }
    }
}
