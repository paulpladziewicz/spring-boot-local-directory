package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.CustomResponse;
import com.paulpladziewicz.fremontmi.models.InvoiceDTO;
import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.models.SubscriptionDTO;
import com.paulpladziewicz.fremontmi.services.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.InvoiceListParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stripe")
public class BillingController {

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    private final StripeService stripeService;

    public BillingController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public static class CancelSubscriptionRequest {
        public String subscriptionId;

        // Optionally add a constructor and getter/setter methods
        public CancelSubscriptionRequest() {}

        public CancelSubscriptionRequest(String subscriptionId) {
            this.subscriptionId = subscriptionId;
        }
    }


    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionDTO>> getSubscriptions() {
        ServiceResponse<List<Subscription>> getSubscriptionsResponse = stripeService.getSubscriptions();

        if (getSubscriptionsResponse.hasError()) {
            return ResponseEntity.status(500).body(null);
        }

        List<SubscriptionDTO> subscriptionDTOs = getSubscriptionsResponse.value().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(subscriptionDTOs);
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceDTO>> getInvoices() {
        // Get the user's invoices using the service layer
        ServiceResponse<List<Invoice>> getInvoicesResponse = stripeService.getInvoices();

        if (getInvoicesResponse.hasError()) {
            return ResponseEntity.status(500).body(null); // Return 500 if there is an error
        }

        // Map the list of invoices to DTOs for the response
        List<InvoiceDTO> invoiceDTOs = getInvoicesResponse.value().stream()
                .map(this::mapInvoiceToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(invoiceDTOs); // Return the mapped invoice DTOs
    }

    @PostMapping("/cancel-subscription")
    public ResponseEntity<CustomResponse> cancelSubscription(@RequestBody CancelSubscriptionRequest request) {
        String subscriptionId = request.subscriptionId;
        ServiceResponse<Boolean> cancelSubscriptionResponse = stripeService.cancelSubscriptionAtPeriodEnd(subscriptionId);

        if (cancelSubscriptionResponse.hasError()) {
            return ResponseEntity.status(500)
                    .body(new CustomResponse(false, cancelSubscriptionResponse.errorCode()));
        }

        return ResponseEntity.ok(new CustomResponse(true, "Subscription cancellation scheduled at period end."));
    }

    @PostMapping("/resume-subscription")
    public ResponseEntity<CustomResponse> resumeSubscription(@RequestBody CancelSubscriptionRequest request) {
        String subscriptionId = request.subscriptionId;
        ServiceResponse<Boolean> resumeSubscriptionResponse = stripeService.resumeSubscription(subscriptionId);

        if (resumeSubscriptionResponse.hasError()) {
            return ResponseEntity.status(500)
                    .body(new CustomResponse(false, resumeSubscriptionResponse.errorCode()));
        }

        return ResponseEntity.ok(new CustomResponse(true, "Subscription resumed successfully."));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        return stripeService.handleStripeWebhook(payload, sigHeader);
    }

    public SubscriptionDTO mapToDTO(Subscription subscription) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setId(subscription.getId());
        dto.setCustomerId(subscription.getCustomer());
        dto.setStatus(subscription.getStatus());
        dto.setCollectionMethod(subscription.getCollectionMethod());
        dto.setCurrency(subscription.getCurrency());

        // Assuming there's always one item in the list; adjust as needed for your use case
        if (!subscription.getItems().getData().isEmpty()) {
            SubscriptionItem item = subscription.getItems().getData().getFirst();
            dto.setAmount(Math.toIntExact(item.getPrice().getUnitAmount()));
            dto.setPlanName(item.getPrice().getProduct());
            dto.setInterval(item.getPrice().getRecurring().getInterval());
            dto.setIntervalCount(Math.toIntExact(item.getPrice().getRecurring().getIntervalCount()));
        }

        dto.setStartDate(subscription.getStartDate());
        dto.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        dto.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        dto.setCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());
        dto.setCanceledAt(subscription.getCanceledAt());
        dto.setLatestInvoice(subscription.getLatestInvoice());

        // Extract payment methods
        dto.setPaymentMethods(subscription.getPaymentSettings().getPaymentMethodTypes());

        return dto;
    }

    public InvoiceDTO mapInvoiceToDTO(Invoice invoice) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(invoice.getId());
        dto.setCustomerId(invoice.getCustomer());
        dto.setStatus(invoice.getStatus());
        dto.setAmountDue(invoice.getAmountDue());
        dto.setAmountPaid(invoice.getAmountPaid());
        dto.setAmountRemaining(invoice.getAmountRemaining());
        dto.setCreated(invoice.getCreated());
        dto.setCurrency(invoice.getCurrency());

        if (invoice.getPaymentIntentObject() != null) {
            dto.setPaymentIntent(invoice.getPaymentIntentObject().getId());
        }

        return dto;
    }
}

