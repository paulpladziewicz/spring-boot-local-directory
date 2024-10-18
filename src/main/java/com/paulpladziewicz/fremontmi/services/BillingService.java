package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.exceptions.StripeServiceException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.BillingRepository;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.param.SubscriptionUpdateParams.Item;
import com.stripe.model.*;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillingService {

    private static final Logger logger = LoggerFactory.getLogger(BillingService.class);

    private final UserService userService;

    private final EmailService emailService;

    private final BillingRepository billingRepository;

    private final ContentRepository contentRepository;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    @Value("${stripe.price.monthly.neighborservice}")
    private String monthlyNeighborServicePriceId;
    @Value("${stripe.price.annual.neighborservice}")
    private String annualNeighborServicePriceId;
    private String monthlyNeighborServiceDisplayPrice = "$5.00 / month";
    private String monthlyNeighborServiceDisplayName = "Monthly NeighborServices™ Subscription";
    private String annualNeighborServiceDisplayPrice = "$50.00 / year";
    private String annualNeighborServiceDisplayName = "Yearly NeighborServices™ Subscription";

    public BillingService(UserService userService, EmailService emailService, BillingRepository billingRepository, ContentRepository contentRepository) {
        this.userService = userService;
        this.emailService = emailService;
        this.billingRepository = billingRepository;
        this.contentRepository = contentRepository;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public String getCustomerId() {
        UserProfile userProfile = userService.getUserProfile();

        if (userProfile.getStripeCustomerId() != null) {
            return userProfile.getStripeCustomerId();
        }

        return createCustomer(userProfile);
    }

    public String createCustomer(UserProfile userProfile) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setName(userProfile.getFirstName() + ' ' + userProfile.getLastName())
                    .setEmail(userProfile.getEmail())
                    .build();

            Customer customer = Customer.create(params);

            if (customer == null) {
                throw new StripeServiceException("Failed to create a Stripe customer. Stripe returned null.");
            }

            userProfile.setStripeCustomerId(customer.getId());
            userService.saveUserProfile(userProfile);

            return customer.getId();

        } catch (StripeException e) {
            logger.error("Error creating Stripe customer: ", e);
            throw new StripeServiceException("Error occurred while creating a Stripe customer.", e);
        }
    }

    public Map<String, Object> createSubscription(String priceId) {
        String customerId = getCustomerId();

        SubscriptionCreateParams subCreateParams = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build())
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .setPaymentSettings(
                        SubscriptionCreateParams.PaymentSettings.builder()
                                .setPaymentMethodTypes(
                                        java.util.List.of(SubscriptionCreateParams.PaymentSettings.PaymentMethodType.CARD)
                                )
                                .build()
                )
                .addExpand("latest_invoice.payment_intent") // TODO do I need this?
                .build();

        try {
            Subscription subscription = Subscription.create(subCreateParams);

            String clientSecret = subscription.getLatestInvoiceObject()
                    .getPaymentIntentObject()
                    .getClientSecret();

            if (clientSecret == null) {
                logger.error("ClientSecret is null for subscription: {}", subscription.getId());
                throw new StripeServiceException("Failed to retrieve client secret for payment intent.");
            }

            Map<String, Object> subscriptionData = new HashMap<>();
            subscriptionData.put("subscriptionId", subscription.getId());
            subscriptionData.put("clientSecret", clientSecret);

            return subscriptionData;
        } catch (StripeException e) {
            logger.error("Failed to create subscription due to a Stripe exception", e);
            throw new StripeServiceException("Failed to create subscription with Stripe.", e);
        }
    }

    public Map<String, Object> updateActiveSubscription(String subscriptionId, String newPriceId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);

            if (subscription == null) {
                throw new StripeServiceException("Subscription not found for ID: " + subscriptionId);
            }

            SubscriptionUpdateParams updateParams;

            updateParams = SubscriptionUpdateParams.builder()
                    .addItem(Item.builder()
                            .setId(subscription.getItems().getData().getFirst().getId())
                            .setPrice(newPriceId)
                            .build())
                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                    .addExpand("latest_invoice.payment_intent")
                    .build();

            subscription.update(updateParams);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);

            return result;
        } catch (StripeException e) {
            logger.error("Error updating subscription: ", e);
            throw new StripeServiceException("Failed to update subscription with Stripe.", e);
        }
    }

    public List<Subscription> getSubscriptions() {
        UserProfile userProfile = userService.getUserProfile();
        String stripeCustomerId = userProfile.getStripeCustomerId();

        if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            SubscriptionListParams params = SubscriptionListParams.builder()
                    .setStatus(SubscriptionListParams.Status.ACTIVE) // Only fetch active subscriptions
                    .setCustomer(userProfile.getStripeCustomerId())
                    .addExpand("data.default_payment_method")
                    .addExpand("data.latest_invoice") // Ensure latest invoice is fetched
                    .build();

            SubscriptionCollection subscriptions = Subscription.list(params);

            return subscriptions.getData().stream()
                    .filter(subscription -> "active".equals(subscription.getStatus())) // Check active status correctly
                    .filter(subscription -> {
                        Invoice latestInvoice = subscription.getLatestInvoiceObject(); // Use getLatestInvoiceObject()
                        return latestInvoice != null && "paid".equals(latestInvoice.getStatus());
                    })
                    .collect(Collectors.toList());
        } catch (StripeException e) {
            logger.error("Failed to retrieve subscriptions due to a Stripe exception", e);
            throw new StripeServiceException("Failed to retrieve subscriptions with Stripe.", e);
        }
    }

    public List<Invoice> getInvoices() {
        UserProfile userProfile = userService.getUserProfile();
        String stripeCustomerId = userProfile.getStripeCustomerId();

        if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            InvoiceListParams params = InvoiceListParams.builder()
                    .setCustomer(userProfile.getStripeCustomerId())
                    .build();

            InvoiceCollection invoiceCollection = Invoice.list(params);

            return invoiceCollection.getData().stream()
                    .filter(invoice -> "paid".equals(invoice.getStatus())) // Check if the invoice status is 'paid'
                    .collect(Collectors.toList());
        } catch (StripeException e) {
            logger.error("Failed to retrieve invoices due to a Stripe exception", e);
            throw new StripeServiceException("Failed to retrieve invoices with Stripe.", e);
        }
    }

    public void cancelSubscriptionAtPeriodEnd(String subscriptionId) {
        UserProfile userProfile = userService.getUserProfile();

        if (userProfile.getStripeCustomerId() == null) {
            throw new StripeServiceException("No Stripe customer ID associated with this user.");
        }

        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);

            SubscriptionUpdateParams updateParams = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .build();

            subscription.update(updateParams);

        } catch (StripeException e) {
            logger.error("Error setting cancel_at_period_end flag for subscription: ", e);
            throw new StripeServiceException("Failed to cancel subscription at period end.", e);
        }
    }

    public void resumeSubscription(String subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);

            if (subscription.getCancelAtPeriodEnd()) {
                // Unset the cancel_at_period_end flag
                SubscriptionUpdateParams updateParams = SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(false)
                        .build();

                subscription.update(updateParams);

            } else {
                throw new StripeServiceException("Subscription is not set to cancel at period end.");
            }

        } catch (StripeException e) {
            logger.error("Error resuming subscription: ", e);
            throw new StripeServiceException("Failed to resume subscription with Stripe.", e);
        }
    }


    public ResponseEntity<String> handleStripeWebhook(String payload, String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

            switch (event.getType()) {
                case "invoice.payment_failed":
                    Invoice failedInvoice = (Invoice) dataObjectDeserializer.getObject().orElse(null);
                    if (failedInvoice != null) {
                        handlePaymentFailed(failedInvoice);
                        // there is subscription data within; may not need invoice is
                        System.out.println("Payment failed");
                    }
                    break;

                case "customer.subscription.deleted":
                    Subscription subscription = (Subscription) dataObjectDeserializer.getObject().orElse(null);
                    if (subscription != null) {
                        handleSubscriptionCancellation(subscription);
                        System.out.println("Subscription canceled");
                    }
                    break;

                case "charge.dispute.created":
                    Dispute dispute = (Dispute) dataObjectDeserializer.getObject().orElse(null);
                    if (dispute != null) {
                        emailService.sendDisputeCreatedEmailAsync("ppladziewicz@gmail.com", dispute);
                        logger.info("Dispute created and email sent for dispute ID {}", dispute.getId());
                    }
                    break;
            }

            return ResponseEntity.ok("");
        } catch (SignatureVerificationException e) {
            // Invalid signature error, reject the request
            logger.error("Invalid Stripe webhook signature: ", e);
            return ResponseEntity.status(400).body("Invalid signature");
        } catch (Exception e) {
            // General error while processing the event
            logger.error("Error handling Stripe webhook: ", e);
            return ResponseEntity.status(400).body("Webhook handling error");
        }
    }

    private void handleSubscriptionCancellation(Subscription subscription) {
        String subscriptionId = subscription.getId();

        // Retrieve the content by subscriptionId from the repository
        Optional<Content> optionalContent = contentRepository.findByStripeDetails_SubscriptionId(subscriptionId);

        if (optionalContent.isPresent()) {
            Content content = optionalContent.get();

            // Mark the content or subscription as canceled, you can set a status field or similar
            content.setStatus(ContentStatus.REQUIRES_ACTIVE_SUBSCRIPTION.getStatus());
            content.setVisibility(ContentVisibility.RESTRICTED.getVisibility());
            contentRepository.save(content);

            logger.info("Subscription {} canceled and content updated.", subscriptionId);
        } else {
            logger.warn("Content not found for subscription ID {}", subscriptionId);
        }
    }

    private void handlePaymentFailed(Invoice failedInvoice) {
        String invoiceId = failedInvoice.getId();

        Optional<Content> optionalContent = contentRepository.findByStripeDetails_InvoiceId(invoiceId);

        if (optionalContent.isPresent()) {
            Content content = optionalContent.get();

            content.setStatus(ContentStatus.PAYMENT_FAILED.getStatus());
            contentRepository.save(content);

            logger.info("Payment failed for invoice {} and content updated.", invoiceId);
        } else {
            logger.warn("Content not found for invoice ID {}", invoiceId);
        }
    }

    public String handleSubscriptionSuccess(ConfirmSubscriptionRequest confirmSubscriptionRequest) {
        String paymentStatus = confirmSubscriptionRequest.getPaymentStatus();

        if (!"succeeded".equals(paymentStatus)) {
            throw new StripeServiceException("Payment intent shows failure");
        }

        Optional<Content> optionalContent = contentRepository.findById(confirmSubscriptionRequest.getContentId());

        if (optionalContent.isEmpty()) {
            throw new StripeServiceException("Content not found when confirming subscription");
        }

        Content content = optionalContent.get();

        UserProfile userProfile = userService.getUserProfile();

        StripeSubscriptionRecord subscriptionRecord = new StripeSubscriptionRecord();
        subscriptionRecord.setContentId(confirmSubscriptionRequest.getContentId());
        subscriptionRecord.setContentType(confirmSubscriptionRequest.getContentType());
        subscriptionRecord.setUserId(userProfile.getUserId());
        subscriptionRecord.setStripeCustomerId(userProfile.getStripeCustomerId());
        subscriptionRecord.setSubscriptionId(confirmSubscriptionRequest.getSubscriptionId());
        subscriptionRecord.setPriceId(confirmSubscriptionRequest.getPriceId());
        subscriptionRecord.setDisplayName(confirmSubscriptionRequest.getDisplayName());
        subscriptionRecord.setDisplayPrice(confirmSubscriptionRequest.getDisplayPrice());
        subscriptionRecord.setStatus(paymentStatus);

        billingRepository.save(subscriptionRecord);

        content.setStatus(ContentStatus.ACTIVE.getStatus());
        content.setVisibility(ContentVisibility.PUBLIC.getVisibility());

        Content savedContent = contentRepository.save(content);

        return savedContent.getPathname();
    }

    public Map<String, Object> getPricing(String contentType) {
        Map<String, Object> pricingData = new HashMap<>();

        if (contentType.equalsIgnoreCase("neighbor-services-profile")) {
            Map<String, Object> monthlyPlan = new HashMap<>();
            monthlyPlan.put("priceId", monthlyNeighborServicePriceId);
            monthlyPlan.put("displayPrice", monthlyNeighborServiceDisplayPrice);
            monthlyPlan.put("displayName", monthlyNeighborServiceDisplayName);

            Map<String, Object> annualPlan = new HashMap<>();
            annualPlan.put("priceId", annualNeighborServicePriceId);
            annualPlan.put("displayPrice", annualNeighborServiceDisplayPrice);
            annualPlan.put("displayName", annualNeighborServiceDisplayName);

            pricingData.put("monthly", monthlyPlan);
            pricingData.put("annual", annualPlan);
        } else {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }

        return pricingData;
    }
}

