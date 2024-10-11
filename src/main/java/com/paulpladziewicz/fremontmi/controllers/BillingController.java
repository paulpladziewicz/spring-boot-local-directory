package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.services.BillingService;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stripe")
public class BillingController {

    @Value("${stripe.publishable.key}")
    private String stripePublicKey;

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    public static class CancelSubscriptionRequest {
        public String subscriptionId;
    }

    @GetMapping("/pay/subscription")
    public String paySubscription(Model model) {
        model.addAttribute("stripePublicKey", stripePublicKey);
        return "stripe/pay-subscription";
    }

    @PostMapping("/subscription-payment-success")
    @ResponseBody
    public ResponseEntity<String> subscriptionPaymentSuccess(@RequestBody PaymentRequest paymentRequest) {
        ServiceResponse<Content> serviceResponse = billingService.handleSubscriptionSuccess(paymentRequest);

        if (serviceResponse.hasError()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        Content content = serviceResponse.value();

        String redirectUrl = content.getPathname() + "?subscribed=true";

        return ResponseEntity.ok("{\"redirectUrl\": \"" + redirectUrl + "\"}");
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionDTO>> getSubscriptions() {
        List<SubscriptionDTO> subscriptionDTOs = billingService.getSubscriptions().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(subscriptionDTOs);
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceDTO>> getInvoices() {
        List<InvoiceDTO> invoiceDTOs = billingService.getInvoices().stream()
                .map(this::mapInvoiceToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(invoiceDTOs);
    }

    @PostMapping("/cancel-subscription")
    public ResponseEntity<CustomResponse> cancelSubscription(@RequestBody CancelSubscriptionRequest request) {
        String subscriptionId = request.subscriptionId;
        billingService.cancelSubscriptionAtPeriodEnd(subscriptionId);

        return ResponseEntity.ok(new CustomResponse(true, "Subscription cancellation scheduled at period end."));
    }

    @PostMapping("/resume-subscription")
    public ResponseEntity<CustomResponse> resumeSubscription(@RequestBody CancelSubscriptionRequest request) {
        String subscriptionId = request.subscriptionId;
        billingService.resumeSubscription(subscriptionId);

        return ResponseEntity.ok(new CustomResponse(true, "Subscription resumed successfully."));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        return billingService.handleStripeWebhook(payload, sigHeader);
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

        dto.setId(invoice.getId());

        String planName = invoice.getLines().getData().getFirst().getDescription();
        dto.setPlanName(planName);

        dto.setAmountPaid(invoice.getAmountPaid());

        dto.setCustomerName(invoice.getCustomerName());

        Long paidAtUnix = invoice.getStatusTransitions().getPaidAt();
        if (paidAtUnix != null) {
            String paidDate = Instant.ofEpochSecond(paidAtUnix)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
            dto.setPaidDate(paidDate);
        }

        dto.setInvoiceUrl(invoice.getHostedInvoiceUrl() != null ? invoice.getHostedInvoiceUrl() : invoice.getInvoicePdf());

        return dto;
    }
}

