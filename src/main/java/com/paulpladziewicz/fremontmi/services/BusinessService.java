package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Business;
import com.paulpladziewicz.fremontmi.models.PaymentRequest;
import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.repositories.BusinessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
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
            return ServiceResponse.value(businessRepository.save(business));
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

        business.setPaymentIntentId(paymentIntentId);
        business.setPaymentStatus(paymentStatus);

        if (paymentStatus.equals("succeeded")) {
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
            logger.error("Database access error when trying to create a business", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error when trying to create a business", e);
            return Optional.empty();
        }
    }
}
