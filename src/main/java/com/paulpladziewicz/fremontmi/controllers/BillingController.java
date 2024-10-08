package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.CustomResponse;
import com.paulpladziewicz.fremontmi.models.InvoiceDTO;
import com.paulpladziewicz.fremontmi.models.SubscriptionDTO;
import com.paulpladziewicz.fremontmi.services.StripeService;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
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
        List<SubscriptionDTO> subscriptionDTOs = stripeService.getSubscriptions().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(subscriptionDTOs);
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceDTO>> getInvoices() {
        List<InvoiceDTO> invoiceDTOs = stripeService.getInvoices().stream()
                .map(this::mapInvoiceToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(invoiceDTOs);
    }

    @PostMapping("/cancel-subscription")
    public ResponseEntity<CustomResponse> cancelSubscription(@RequestBody CancelSubscriptionRequest request) {
        String subscriptionId = request.subscriptionId;
        stripeService.cancelSubscriptionAtPeriodEnd(subscriptionId);

        return ResponseEntity.ok(new CustomResponse(true, "Subscription cancellation scheduled at period end."));
    }

    @PostMapping("/resume-subscription")
    public ResponseEntity<CustomResponse> resumeSubscription(@RequestBody CancelSubscriptionRequest request) {
        String subscriptionId = request.subscriptionId;
        stripeService.resumeSubscription(subscriptionId);

        return ResponseEntity.ok(new CustomResponse(true, "Subscription resumed successfully."));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        return stripeService.handleStripeWebhook(payload, sigHeader);
    }

    private SubscriptionDTO mapToDTO(Subscription subscription) {
        SubscriptionDTO dto = new SubscriptionDTO();

        dto.setId(subscription.getId());

        String planName = subscription.getItems().getData().getFirst().getPrice().getMetadata().get("displayName");
        String price = subscription.getItems().getData().getFirst().getPrice().getMetadata().get("displayPrice");

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


    private InvoiceDTO mapInvoiceToDTO(Invoice invoice) {
        InvoiceDTO dto = new InvoiceDTO();

        // Set the invoice ID
        dto.setId(invoice.getId());

        // Extract plan name from the line item description
        String planName = invoice.getLines().getData().getFirst().getDescription();
        dto.setPlanName(planName);

        // Amount paid (in smallest currency unit, e.g., cents)
        dto.setAmountPaid(invoice.getAmountPaid());

        // Customer name
        dto.setCustomerName(invoice.getCustomerName());

        // Convert the paid timestamp to a human-readable date format
        Long paidAtUnix = invoice.getStatusTransitions().getPaidAt();
        if (paidAtUnix != null) {
            String paidDate = Instant.ofEpochSecond(paidAtUnix)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
            dto.setPaidDate(paidDate);
        }

        // Set the invoice URL (hosted_invoice_url or invoice_pdf)
        dto.setInvoiceUrl(invoice.getHostedInvoiceUrl() != null ? invoice.getHostedInvoiceUrl() : invoice.getInvoicePdf());

        return dto;
    }
}

