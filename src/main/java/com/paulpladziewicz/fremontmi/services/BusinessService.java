package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Business;
import com.paulpladziewicz.fremontmi.models.PaymentRequest;
import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.models.UserProfile;
import com.paulpladziewicz.fremontmi.repositories.BusinessRepository;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BusinessService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessService.class);

    private final BusinessRepository businessRepository;

    private final UserService userService;
    private final StripeService stripeService;

    public BusinessService(BusinessRepository businessRepository, UserService userService, StripeService stripeService) {
        this.businessRepository = businessRepository;
        this.userService = userService;
        this.stripeService = stripeService;
    }

    public ServiceResponse<Business> createBusiness(Business business) {
        ServiceResponse<Map<String, Object>> serviceResponse = stripeService.createSubscription(business.getSubscriptionPriceId());

        if (serviceResponse.hasError()) {
            return ServiceResponse.error(serviceResponse.errorCode());
        }

        business.setClientSecret((String) serviceResponse.value().get("clientSecret"));
        business.setStripeSubscriptionId((String) serviceResponse.value().get("subscriptionId"));

        try {
            Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

            if (optionalUserProfile.isEmpty()) {
                return ServiceResponse.error("user_profile_not_found");
            }

            UserProfile userProfile = optionalUserProfile.get();

            ServiceResponse<Business> saveBusinessResponse = ServiceResponse.value(businessRepository.save(business));

            if (saveBusinessResponse.hasError()) {
                return ServiceResponse.error(saveBusinessResponse.errorCode());
            }

            Business savedBusiness = saveBusinessResponse.value();

            userProfile.getBusinessIds().add(savedBusiness.getId());

            ServiceResponse<UserProfile> saveUserProfileResponse =  userService.saveUserProfile(userProfile);

            if (saveUserProfileResponse.hasError()) {
                return ServiceResponse.error(saveUserProfileResponse.errorCode());
            }

            return ServiceResponse.value(savedBusiness);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to create a business", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to create a business", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public ServiceResponse<Business> handleSubscriptionSuccess(PaymentRequest paymentRequest) {
        String businessId = paymentRequest.getId();
        String paymentIntentId = paymentRequest.getPaymentIntentId();
        String paymentStatus = paymentRequest.getPaymentStatus();

        if (businessId == null || businessId.isEmpty() || paymentIntentId == null || paymentIntentId.isEmpty() || paymentStatus == null || paymentStatus.isEmpty()) {
            return ServiceResponse.error("required_info_not_correct");
        }

        Optional<Business> optionalBusiness = businessRepository.findById(businessId);

        if (optionalBusiness.isEmpty()) {
            return ServiceResponse.error("business_not_found");
        }

        Business business = optionalBusiness.get();

        ServiceResponse<PaymentIntent> serviceResponse = stripeService.retrievePaymentIntent(paymentIntentId);
        if (serviceResponse.hasError()) {
            return ServiceResponse.error(serviceResponse.errorCode());
        }

        PaymentIntent paymentIntent = serviceResponse.value();

        business.setPaymentIntentId(paymentIntent.getId());
        business.setPaymentStatus(paymentIntent.getStatus());

        if (paymentIntent.getStatus().equals("succeeded")) {
            business.setStatus("active");
        } else {
            business.setStatus("inactive");
        }

        try {
            return ServiceResponse.value(businessRepository.save(business));
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to create a business", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to create a business", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public ServiceResponse<List<Business>> findAllActiveBusinesses() {
        try {
            List<Business> activeBusinesses = businessRepository.findByStatus("active");
            return ServiceResponse.value(activeBusinesses);
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
            return businessRepository.findById(businessId);
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
            return ServiceResponse.value(businessRepository.findAllById(userProfile.getBusinessIds()));
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
            return ServiceResponse.value(businessRepository.save(business));
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

        if (business.getStripeSubscriptionId() != null && !business.getStripeSubscriptionId().isEmpty()) {
            ServiceResponse<Boolean> isSubscriptionActiveResponse = stripeService.isSubscriptionActive(business.getStripeSubscriptionId());

            if (isSubscriptionActiveResponse.hasError()) {
                return ServiceResponse.error(isSubscriptionActiveResponse.errorCode());
            }

            if (isSubscriptionActiveResponse.value()) {
                return ServiceResponse.error("subscription_still_active");
            }
        }

        try {
            businessRepository.deleteById(businessId);
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
