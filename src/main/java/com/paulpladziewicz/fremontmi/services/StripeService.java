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

import java.awt.desktop.OpenFilesEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    public ServiceResponse<String> getCustomerId() {
        Optional<UserProfile> userProfileOptional = userService.getUserProfile();

        if (userProfileOptional.isEmpty()) {
            return ServiceResponse.error("No user profile found");
        }

        UserProfile userProfile = userProfileOptional.get();

        if (userProfile.getStripeCustomerId() != null) {
            return ServiceResponse.value(userProfile.getStripeCustomerId());
        }

        ServiceResponse<String> serviceResponse = createCustomer(userProfile);

        if (serviceResponse.hasError()) {
            return serviceResponse;
        }

        String customerId = serviceResponse.value();

        return ServiceResponse.value(customerId);
    }

    public ServiceResponse<PaymentIntent> retrievePaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return ServiceResponse.value(paymentIntent);
        } catch (StripeException e) {
            logger.error("Error retrieving payment intent from Stripe: ", e);
            return ServiceResponse.error("STRIPE_PAYMENT_INTENT_RETRIEVAL_FAILED");
        }
    }

    public ServiceResponse<String> createCustomer(UserProfile userProfile) {
        Customer customer = null;
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setName(userProfile.getFirstName() + ' ' + userProfile.getLastName())
                    .setEmail(userProfile.getEmail())
                    .build();
            customer = Customer.create(params);
        } catch (StripeException e) {
            logger.error("Error creating Stripe customer: ", e);
            return ServiceResponse.error("STRIPE_CUSTOMER_CREATION_FAILED");
        }

        if (customer == null) {
            return ServiceResponse.error("STRIPE_CUSTOMER_CREATION_FAILED");
        }

        userProfile.setStripeCustomerId(customer.getId());

        ServiceResponse<UserProfile> saveUserProfile = userService.saveUserProfile(userProfile);

        if (saveUserProfile.hasError()) {
            logger.error("Failed to save user profile when creating a Stripe customer");
            ServiceResponse.error(saveUserProfile.errorCode());
        }

        return ServiceResponse.value(customer.getId());
    }

    public ServiceResponse<Map<String, Object>> createSubscription(String priceId) {
        ServiceResponse<String> serviceResponse = getCustomerId();

        if (serviceResponse.hasError()) {
            return ServiceResponse.error(serviceResponse.errorCode());
        }

        String customerId = serviceResponse.value();

        SubscriptionCreateParams subCreateParams = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build())
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .setPaymentSettings(
                        SubscriptionCreateParams.PaymentSettings.builder()
                                .setPaymentMethodTypes(
                                        java.util.List.of(SubscriptionCreateParams.PaymentSettings.PaymentMethodType.CARD)  // Use PaymentMethodType enum
                                )
                                .build()
                )
                .addExpand("latest_invoice.payment_intent")
                .build();

        try {
            Subscription subscription = Subscription.create(subCreateParams);

            Map<String, Object> subscriptionData = new HashMap<>();
            subscriptionData.put("subscriptionId", subscription.getId());

            String clientSecret = subscription.getLatestInvoiceObject()
                    .getPaymentIntentObject()
                    .getClientSecret();

            if (clientSecret == null) {
                logger.error("ClientSecret is null for subscription: {}", subscription.getId());
                return ServiceResponse.error("payment_intent_error");
            }

            subscriptionData.put("clientSecret", clientSecret);

            return ServiceResponse.value(subscriptionData);
        } catch (StripeException e) {
            logger.error("Failed to create subscription due to a Stripe exception", e);
            return ServiceResponse.error("stripe_error");
        }
    }

    public ServiceResponse<Boolean> isSubscriptionActive(String subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            return ServiceResponse.value(subscription.getStatus().equals("active"));
        } catch (StripeException e) {
            logger.error("Error retrieving subscription from Stripe: ", e);
            return ServiceResponse.error("STRIPE_SUBSCRIPTION_RETRIEVAL_FAILED");
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

