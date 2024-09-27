package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    private final UserService userService;

    private final EmailService emailService;

    private final ContentRepository contentRepository;

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    public StripeService(UserService userService, EmailService emailService, ContentRepository contentRepository) {
        this.userService = userService;
        this.emailService = emailService;
        this.contentRepository = contentRepository;
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
                return ServiceResponse.error("payment_intent_error");
            }

            Map<String, Object> subscriptionData = new HashMap<>();
            subscriptionData.put("subscriptionId", subscription.getId());
            subscriptionData.put("clientSecret", clientSecret);

            if (subscription.getItems() != null && !subscription.getItems().getData().isEmpty()) {
                SubscriptionItem subscriptionItem = subscription.getItems().getData().getFirst(); // First item

                Price price = subscriptionItem.getPrice();

                String displayName = price.getMetadata().get("displayName");
                String displayPrice = price.getMetadata().get("displayPrice");

                if (displayName != null) {
                    subscriptionData.put("displayName", displayName);
                }
                if (displayPrice != null) {
                    subscriptionData.put("displayPrice", displayPrice);
                }

                subscriptionData.put("priceId", price.getId());
            }

            Invoice latestInvoice = subscription.getLatestInvoiceObject();
            if (latestInvoice != null) {
                subscriptionData.put("invoiceId", latestInvoice.getId());
            } else {
                logger.warn("Latest invoice is null for subscription: {}", subscription.getId());
            }

            // Compute the time remaining until the subscription auto-cancels
            // We can use the billing_cycle_anchor to understand the time left
//            long billingCycleAnchor = subscription.getBillingCycleAnchor();
//            long currentTimestamp = System.currentTimeMillis() / 1000; // Current time in seconds
//            long timeUntilBillingCycleAnchor = billingCycleAnchor - currentTimestamp;

            // You may decide to consider subscriptions "too close" if they're within a certain time frame (e.g., 48 hours)
//            long thresholdTimeInSeconds = 48 * 3600; // 48 hours in seconds
//            boolean isTooCloseToAutoCancel = timeUntilBillingCycleAnchor <= thresholdTimeInSeconds;

//            subscriptionData.put("timeUntilBillingCycleAnchor", timeUntilBillingCycleAnchor);
//            subscriptionData.put("isTooCloseToAutoCancel", isTooCloseToAutoCancel);

            return ServiceResponse.value(subscriptionData);
        } catch (StripeException e) {
            logger.error("Failed to create subscription due to a Stripe exception", e);
            return ServiceResponse.error("stripe_error");
        }
    }

    public ServiceResponse<List<Subscription>> getSubscriptions() {
        Optional<UserProfile> userProfileOptional = userService.getUserProfile();

        if (userProfileOptional.isEmpty()) {
            return ServiceResponse.error("No user profile found");
        }

        UserProfile userProfile = userProfileOptional.get();
        String stripeCustomerId = userProfile.getStripeCustomerId();

        if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            return ServiceResponse.value(Collections.emptyList());
        }

        try {
            SubscriptionListParams params = SubscriptionListParams.builder()
                    .setStatus(SubscriptionListParams.Status.ACTIVE) // Only fetch active subscriptions
                    .setCustomer(userProfile.getStripeCustomerId())
                    .addExpand("data.default_payment_method")
                    .addExpand("data.latest_invoice") // Ensure latest invoice is fetched
                    .build();

            SubscriptionCollection subscriptions = Subscription.list(params);

            // Filter subscriptions that have a 'paid' invoice status
            List<Subscription> activePaidSubscriptions = subscriptions.getData().stream()
                    .filter(subscription -> "active".equals(subscription.getStatus())) // Check active status correctly
                    .filter(subscription -> {
                        Invoice latestInvoice = subscription.getLatestInvoiceObject(); // Use getLatestInvoiceObject()
                        return latestInvoice != null && "paid".equals(latestInvoice.getStatus());
                    })
                    .collect(Collectors.toList());

            return ServiceResponse.value(activePaidSubscriptions);
        } catch (StripeException e) {
            logger.error("Error retrieving subscriptions from Stripe: ", e);
            return ServiceResponse.error("STRIPE_SUBSCRIPTION_RETRIEVAL_FAILED");
        }
    }

    public ServiceResponse<List<Invoice>> getInvoices() {
        Optional<UserProfile> userProfileOptional = userService.getUserProfile();

        if (userProfileOptional.isEmpty()) {
            return ServiceResponse.error("No user profile found");
        }

        UserProfile userProfile = userProfileOptional.get();

        String stripeCustomerId = userProfile.getStripeCustomerId();

        if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            return ServiceResponse.value(Collections.emptyList());
        }

        try {
            InvoiceListParams params = InvoiceListParams.builder()
                    .setCustomer(userProfile.getStripeCustomerId())
                    .build();

            InvoiceCollection invoiceCollection = Invoice.list(params);

            return ServiceResponse.value(invoiceCollection.getData());
        } catch (StripeException e) {
            logger.error("Error retrieving invoices from Stripe: ", e);
            return ServiceResponse.error("STRIPE_INVOICE_RETRIEVAL_FAILED");
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

    public ServiceResponse<Boolean> cancelSubscriptionAtPeriodEnd(String subscriptionId) {
        Optional<UserProfile> userProfileOptional = userService.getUserProfile();

        if (userProfileOptional.isEmpty()) {
            return ServiceResponse.error("No user profile found");
        }

        UserProfile userProfile = userProfileOptional.get();

        if (userProfile.getStripeCustomerId() == null) {
            return ServiceResponse.error("No Stripe customer ID found");
        }

        try {
            // Retrieve the subscription
            Subscription subscription = Subscription.retrieve(subscriptionId);

            // Set the cancel_at_period_end flag to true
            SubscriptionUpdateParams updateParams = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .build();

            subscription.update(updateParams);
            return ServiceResponse.value(true);
        } catch (StripeException e) {
            logger.error("Error setting cancel_at_period_end flag for subscription: ", e);
            return ServiceResponse.error("STRIPE_SUBSCRIPTION_CANCELLATION_FAILED");
        }
    }

    public ServiceResponse<Boolean> resumeSubscription(String subscriptionId) {
        try {
            // Retrieve the subscription
            Subscription subscription = Subscription.retrieve(subscriptionId);

            if (subscription.getCancelAtPeriodEnd()) {
                // Unset the cancel_at_period_end flag
                SubscriptionUpdateParams updateParams = SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(false)
                        .build();

                subscription.update(updateParams);
                return ServiceResponse.value(true);
            } else {
                return ServiceResponse.error("Subscription is not set to cancel at period end.");
            }
        } catch (StripeException e) {
            logger.error("Error resuming subscription: ", e);
            return ServiceResponse.error("STRIPE_SUBSCRIPTION_RESUME_FAILED");
        }
    }

    public ResponseEntity<String> handleStripeWebhook(String payload, String sigHeader) {
        String webhookSecret = "whsec_d4808bcf47b6151388a0ea88df2544f4a1f16072523a4ae7fecbef42c6d90250";  // Replace with your actual secret
        try {
            // Verify the Stripe event
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            // Deserialize the event's data object
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

            // Handle different types of events
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

        // Retrieve the content by invoiceId from the repository
        Optional<Content> optionalContent = contentRepository.findByStripeDetails_InvoiceId(invoiceId);

        if (optionalContent.isPresent()) {
            Content content = optionalContent.get();

            // Mark the content or subscription as having failed payment, e.g., set status or flag
            content.setStatus(ContentStatus.PAYMENT_FAILED.getStatus());
            contentRepository.save(content);

            logger.info("Payment failed for invoice {} and content updated.", invoiceId);
        } else {
            logger.warn("Content not found for invoice ID {}", invoiceId);
        }
    }

    public ServiceResponse<Content> handleSubscriptionSuccess(PaymentRequest paymentRequest) {
        String contentId = paymentRequest.getEntityId();
        String paymentIntentId = paymentRequest.getPaymentIntentId();
        String paymentStatus = paymentRequest.getPaymentStatus();

        if (!paymentStatus.equals("succeeded")) {
            logger.error("Payment not successful but it should have been when handling successful payment. Payment status: {} neighborServiceProfileId: {}", paymentStatus, paymentRequest.getEntityId());
            return ServiceResponse.error("payment_not_successful");
        }

        Optional<Content> optionalContent = contentRepository.findById(contentId);

        if (optionalContent.isEmpty()) {
            return ServiceResponse.error("content_not_found");
        }

        Content content = optionalContent.get();
        content.setStatus(ContentStatus.ACTIVE.getStatus());
        content.setVisibility(ContentVisibility.PUBLIC.getVisibility());

        try {
            Content savedContent = contentRepository.save(content);

            return ServiceResponse.value(savedContent);
        } catch (Exception e) {
            logger.error("Error saving content after handling successful payment: ", e);
            return ServiceResponse.error("content_save_error");
        }
    }

    public ServiceResponse<Map<String, Object>> getContentDetails(String contentId) {
        Optional<Content> optionalContent = contentRepository.findById(contentId);

        if (optionalContent.isEmpty()) {
            return ServiceResponse.error("content_not_found");
        }

        Content content = optionalContent.get();
        Map<String, Object> stripeDetails = content.getStripeDetails();
        String subscriptionId = (String) stripeDetails.get("subscriptionId");

        if (subscriptionId != null && !subscriptionId.isEmpty()) {
            try {
                // TODO: I could potentially store the timestamps in the database to avoid a stripe call
                Subscription subscription = Subscription.retrieve(subscriptionId);

                long billingCycleAnchor = subscription.getBillingCycleAnchor();
                long currentTimestamp = System.currentTimeMillis() / 1000; // Current time in seconds
                long timeUntilBillingCycleAnchor = billingCycleAnchor - currentTimestamp;
                long thresholdTimeInSeconds = 20 * 3600; // 20 hours in seconds
                boolean isTooCloseToAutoCancel = timeUntilBillingCycleAnchor <= thresholdTimeInSeconds;

                if (isTooCloseToAutoCancel) {
                    String priceId = (String) stripeDetails.get("priceId");
                    ServiceResponse<Map<String, Object>> newSubscriptionResponse = createSubscription(priceId);

                    if (newSubscriptionResponse.hasError()) {
                        return ServiceResponse.error(newSubscriptionResponse.errorCode());
                    }

                    Map<String, Object> newSubscriptionData = newSubscriptionResponse.value();
                    stripeDetails.put("subscriptionId", newSubscriptionData.get("subscriptionId"));
                    stripeDetails.put("clientSecret", newSubscriptionData.get("clientSecret"));
                    stripeDetails.put("displayName", newSubscriptionData.get("displayName"));
                    stripeDetails.put("displayPrice", newSubscriptionData.get("displayPrice"));
                    stripeDetails.put("priceId", newSubscriptionData.get("priceId"));
                    stripeDetails.put("invoiceId", newSubscriptionData.get("invoiceId"));

                    content.setStripeDetails(stripeDetails);
                    contentRepository.save(content);

                    return ServiceResponse.value(stripeDetails);
                } else {
                    return ServiceResponse.value(stripeDetails);
                }
            } catch (StripeException e) {
                logger.error("Error retrieving subscription details: ", e);
                return ServiceResponse.error("stripe_subscription_retrieval_error");
            }
        } else {
            return ServiceResponse.error("no_subscription_id_found");
        }
    }
}

