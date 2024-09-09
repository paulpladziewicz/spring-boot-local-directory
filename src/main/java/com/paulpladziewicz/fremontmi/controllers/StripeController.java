package com.paulpladziewicz.fremontmi.controllers;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.SubscriptionListParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;

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

    static class CancelSubscriptionRequest {
        public String subscriptionId;
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

    @GetMapping("/invoices")
    public ResponseEntity<String> getInvoices(@CookieValue(name = "customer") String customerId) throws StripeException {
        // Create the parameters for fetching the invoices
        InvoiceListParams params = InvoiceListParams.builder()
                .setCustomer(customerId) // Filter invoices for this customer
                //.setLimit(10) // Optional: Limit the number of invoices to return
                .build();

        // Fetch the invoices from Stripe
        InvoiceCollection invoices = Invoice.list(params);

        // Return the invoices as JSON
        return ResponseEntity.ok(invoices.toJson());
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

