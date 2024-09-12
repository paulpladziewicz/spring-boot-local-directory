package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.models.StripeTransactionRecord;
import com.paulpladziewicz.fremontmi.models.UserProfile;
import com.paulpladziewicz.fremontmi.repositories.StripeTransactionRecordRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.awt.desktop.OpenFilesEvent;
import java.util.*;

@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    private final StripeTransactionRecordRepository stripeTransactionRecordRepository;

    private final UserService userService;

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    public StripeService(StripeTransactionRecordRepository stripeTransactionRecordRepository, UserService userService) {
        this.stripeTransactionRecordRepository = stripeTransactionRecordRepository;
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

    public ServiceResponse<StripeTransactionRecord> createSubscription(String priceId, String entityId, String entityType) {
        if (priceId == null || priceId.isEmpty() || priceId.trim().isEmpty()) {
            logger.error("Price ID was not provided when trying to create a subscription");
            return ServiceResponse.error("price_id_required");
        }

        if (!priceId.equals("price_1Pv7TzBCHBXtJFxOWmV5PE4h") && !priceId.equals("price_1PELXZBCHBXtJFxO4FchTfAv")) {
            logger.error("Invalid price ID provided when trying to create a subscription");
            return ServiceResponse.error("invalid_price_id");
        }

        ServiceResponse<String> getCustomerIdResponse = getCustomerId();

        if (getCustomerIdResponse.hasError()) {
            logger.error("Failed to get customer ID when trying to create a subscription");
            return ServiceResponse.error(getCustomerIdResponse.errorCode());
        }

        String customerId = getCustomerIdResponse.value();

        // TODO prevent duplicate subscriptions if the entity can use an existing subscription

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
                .addExpand("latest_invoice.payment_intent")
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

            Price price = Price.retrieve(priceId);
            String displayName = price.getMetadata().get("displayName");
            String displayPrice = price.getMetadata().get("displayPrice");

            StripeTransactionRecord stripeTransactionRecord = new StripeTransactionRecord();
            stripeTransactionRecord.setDisplayName(displayName);
            stripeTransactionRecord.setDisplayPrice(displayPrice);
            stripeTransactionRecord.setClientSecret(clientSecret);
            stripeTransactionRecord.setSubscriptionId(subscription.getId());
            stripeTransactionRecord.setCustomerId(customerId);
            stripeTransactionRecord.setEntityId(entityId);
            stripeTransactionRecord.setEntityCollection(entityType);
            stripeTransactionRecord.setPriceId(priceId);

            StripeTransactionRecord savedStripeTransactionRecord = stripeTransactionRecordRepository.save(stripeTransactionRecord);

            return ServiceResponse.value(savedStripeTransactionRecord);
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
                    .setStatus(SubscriptionListParams.Status.ALL)
                    .setCustomer(userProfile.getStripeCustomerId())
                    .addExpand("data.default_payment_method")
                    .build();
            SubscriptionCollection subscriptions = Subscription.list(params);
            return ServiceResponse.value(subscriptions.getData());
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
                        //handlePaymentFailed(failedInvoice);
                        System.out.println("Payment failed");
                    }
                    break;

                case "payment_intent.succeeded":
                    PaymentIntent paymentIntent = (PaymentIntent) dataObjectDeserializer.getObject().orElse(null);
                    if (paymentIntent != null) {
                        //handlePaymentSuccess(paymentIntent);
                        System.out.println("Payment success");
                    }
                    break;

                case "customer.subscription.deleted":
                    Subscription subscription = (Subscription) dataObjectDeserializer.getObject().orElse(null);
                    if (subscription != null) {
                        //handleSubscriptionCancellation(subscription);
                        System.out.println("Subscription canceled");
                    }
                    break;

                case "charge.dispute.created":
                    Dispute dispute = (Dispute) dataObjectDeserializer.getObject().orElse(null);
                    if (dispute != null) {
                        //handleDisputeCreated(dispute);
                        System.out.println("Dispute created");
                    }
                    break;

                default:
                    logger.info("Unhandled event type: {}", event.getType());
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

    public ServiceResponse<Boolean> cancelSubscription(String subscriptionId) {
        Optional<UserProfile> userProfileOptional = userService.getUserProfile();

        if (userProfileOptional.isEmpty()) {
            return ServiceResponse.error("No user profile found");
        }

        UserProfile userProfile = userProfileOptional.get();

        if (userProfile.getStripeCustomerId() == null) {
            return ServiceResponse.error("No Stripe customer ID found");
        }

        try {
            SubscriptionListParams params = SubscriptionListParams.builder()
                    .setStatus(SubscriptionListParams.Status.ALL)
                    .setCustomer(userProfile.getStripeCustomerId())
                    .build();
            SubscriptionCollection subscriptions = Subscription.list(params);

            if (subscriptions.getData().isEmpty()) {
                return ServiceResponse.error("No subscriptions found");
            }

            boolean subscriptionFound = false;

            for (Subscription subscription : subscriptions.getData()) {
                if (subscription.getId().equals(subscriptionId)) {
                    subscriptionFound = true;
                    break;
                }
            }

            if (!subscriptionFound) {
                return ServiceResponse.error("Subscription not found");
            }

            Subscription subscription = Subscription.retrieve(subscriptionId);
            subscription.cancel();
            return ServiceResponse.value(true);
        } catch (StripeException e) {
            logger.error("Error canceling subscription: ", e);
            return ServiceResponse.error("STRIPE_SUBSCRIPTION_CANCELLATION_FAILED");
        }
    }
}

