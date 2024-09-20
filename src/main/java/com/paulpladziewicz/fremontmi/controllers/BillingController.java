package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.CustomResponse;
import com.paulpladziewicz.fremontmi.models.InvoiceDTO;
import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.models.SubscriptionDTO;
import com.paulpladziewicz.fremontmi.services.StripeService;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stripe")
public class BillingController {

    private final StripeService stripeService;

    public BillingController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    public static class CancelSubscriptionRequest {
        public String subscriptionId;
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
        ServiceResponse<List<Invoice>> getInvoicesResponse = stripeService.getInvoices();

        if (getInvoicesResponse.hasError()) {
            return ResponseEntity.status(500).body(null);
        }

        List<InvoiceDTO> invoiceDTOs = getInvoicesResponse.value().stream()
                .map(this::mapInvoiceToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(invoiceDTOs);
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

    private SubscriptionDTO mapToDTO(Subscription subscription) {
        SubscriptionDTO dto = new SubscriptionDTO();

        dto.setId(subscription.getId());

        String planName = subscription.getItems().getData().get(0).getPrice().getMetadata().get("displayName");
        String price = subscription.getItems().getData().get(0).getPrice().getMetadata().get("displayPrice");

        Long nextPaymentUnix = subscription.getCurrentPeriodEnd();
        String nextRecurringPayment = Instant.ofEpochSecond(nextPaymentUnix)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));

        boolean willCancel = subscription.getCancelAtPeriodEnd();
        String subscriptionEnd = willCancel ? "Subscription ends: " + nextRecurringPayment
                : "Next reoccurring payment: " + nextRecurringPayment;

        dto.setPlanName(planName);
        dto.setPrice(price);
        dto.setNextRecurringPayment(nextRecurringPayment);
        dto.setSubscriptionEnd(subscriptionEnd);
        dto.setCancelAtPeriodEnd(willCancel);

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

        dto.setReceiptUrl(invoice.getHostedInvoiceUrl());
        return dto;
    }
}

