package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.models.UserProfile;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionListParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    private final UserService userService;

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    public StripeService(UserService userService) {
        this.userService = userService;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    static class CreateCustomerRequest {
        public String email;
    }

    static class CreateSubscriptionRequest {
        public String priceId;
    }

    static class CancelSubscriptionRequest {
        public String subscriptionId;
    }

    public ResponseEntity<Map<String, Object>> getConfig() throws StripeException {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("publishableKey", stripePublishableKey);

        PriceListParams params = PriceListParams.builder()
                .addLookupKey("neighbor_services_monthly")
                .addLookupKey("neighbor_services_annually")
                .build();
        PriceCollection prices = Price.list(params);
        responseData.put("prices", prices.getData());

        return ResponseEntity.ok(responseData);
    }

    public ServiceResponse<String> createCustomer(UserProfile userProfile) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setName(userProfile.getFirstName() + ' ' + userProfile.getLastName())
                .setEmail(userProfile.getEmail())
                .build();
        Customer customer = Customer.create(params);

        userProfile.setStripeCustomerId(customer.getId());

        ServiceResponse<UserProfile> saveUserProfile = userService.saveUserProfile(userProfile);

        if (saveUserProfile.hasError()) {
            logger.error("Failed to save user profile when creating a Stripe customer");
            ServiceResponse.error(saveUserProfile.errorCode());
        }

        return ServiceResponse.value(customer.getId());
    }

    public ServiceResponse<Map<String, Object>> createSubscription(UserProfile userProfile) {
        SubscriptionCreateParams subCreateParams = SubscriptionCreateParams.builder()
                .setCustomer(userProfile.getStripeCustomerId())
                .addItem(SubscriptionCreateParams.Item.builder().setPrice("price_1Pv7XIBCHBXtJFxOUIvRA6Xf").build())
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .addExpand("latest_invoice.payment_intent")
                .build();

        try {
            Subscription subscription = Subscription.create(subCreateParams);

            Map<String, Object> subscriptionData = new HashMap<>();
            subscriptionData.put("subscriptionId", subscription.getId());
            subscriptionData.put("clientSecret", subscription.getLatestInvoiceObject().getPaymentIntentObject().getClientSecret());

            return ServiceResponse.value(subscriptionData);
        } catch (StripeException e) {
            logger.error("Failed to create subscription due to a Stripe exception", e);
            return ServiceResponse.error("stripe_error");
        }
    }

    public ResponseEntity<String> getSubscriptions(@CookieValue(name = "customer") String customerId) throws StripeException {
        SubscriptionListParams params = SubscriptionListParams.builder()
                .setStatus(SubscriptionListParams.Status.ALL)
                .setCustomer(customerId)
                .addExpand("data.default_payment_method")
                .build();

        SubscriptionCollection subscriptions = Subscription.list(params);

        return ResponseEntity.ok(subscriptions.toJson());
    }

    public ResponseEntity<String> cancelSubscription(@RequestBody CancelSubscriptionRequest request) throws StripeException {
        Subscription subscription = Subscription.retrieve(request.subscriptionId);
        Subscription canceledSubscription = subscription.cancel();

        return ResponseEntity.ok(canceledSubscription.toJson());
    }
}

