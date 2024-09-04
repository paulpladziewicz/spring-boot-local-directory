package com.paulpladziewicz.fremontmi.controllers;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionListParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    // DTO classes for request bodies
    static class CreateCustomerRequest {
        public String email;
    }

    static class CreateSubscriptionRequest {
        public String priceId;
    }

    static class CancelSubscriptionRequest {
        public String subscriptionId;
    }

    @GetMapping("/config")
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

    @PostMapping("/create-customer")
    public ResponseEntity<Map<String, Object>> createCustomer(@RequestBody CreateCustomerRequest request) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(request.email)
                .build();
        Customer customer = Customer.create(params);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("customer", customer.getId());

        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/create-subscription")
    public ResponseEntity<Map<String, Object>> createSubscription(@RequestBody CreateSubscriptionRequest request, @CookieValue(name = "customer") String customerId) throws StripeException {
        SubscriptionCreateParams subCreateParams = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder().setPrice(request.priceId).build())
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .addExpand("latest_invoice.payment_intent")
                .build();

        Subscription subscription = Subscription.create(subCreateParams);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("subscriptionId", subscription.getId());
        responseData.put("clientSecret", subscription.getLatestInvoiceObject().getPaymentIntentObject().getClientSecret());

        return ResponseEntity.ok(responseData);
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<String> getSubscriptions(@CookieValue(name = "customer") String customerId) throws StripeException {
        SubscriptionListParams params = SubscriptionListParams.builder()
                .setStatus(SubscriptionListParams.Status.ALL)
                .setCustomer(customerId)
                .addExpand("data.default_payment_method")
                .build();

        SubscriptionCollection subscriptions = Subscription.list(params);

        return ResponseEntity.ok(subscriptions.toJson());
    }

    @PostMapping("/cancel-subscription")
    public ResponseEntity<String> cancelSubscription(@RequestBody CancelSubscriptionRequest request) throws StripeException {
        Subscription subscription = Subscription.retrieve(request.subscriptionId);
        Subscription canceledSubscription = subscription.cancel();

        return ResponseEntity.ok(canceledSubscription.toJson());
    }

    // Webhook for Stripe events
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        // Webhook handling code...
        return ResponseEntity.ok("");
    }
}

