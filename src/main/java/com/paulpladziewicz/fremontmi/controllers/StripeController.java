package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Content;
import com.paulpladziewicz.fremontmi.models.PaymentRequest;
import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.services.StripeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class StripeController {

    @Value("${stripe.publicKey}")
    private String stripePublicKey;

    private final StripeService stripeService;

    public StripeController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @GetMapping("/pay/subscription")
    public String paySubscription(Model model) {
        return "stripe/pay-subscription";
    }

    @GetMapping("/api/get-content/{contentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getContent(@PathVariable String contentId) {
        ServiceResponse<Map<String, Object>> serviceResponse = stripeService.getContentDetails(contentId);

        if (serviceResponse.hasError()) {
            switch (serviceResponse.errorCode()) {
                case "content_not_found":
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Content not found."));
                case "subscription_inactive":
                    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(Map.of("error", "Your subscription is no longer active. Please renew."));
                case "no_subscription_id_found":
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "No subscription details found for this content."));
                default:
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
            }
        }

        return ResponseEntity.ok(serviceResponse.value());
    }

    @PostMapping("/subscription-payment-success")
    @ResponseBody
    public ResponseEntity<String> subscriptionPaymentSuccess(@RequestBody PaymentRequest paymentRequest) {
        ServiceResponse<Content> serviceResponse = stripeService.handleSubscriptionSuccess(paymentRequest);

        if (serviceResponse.hasError()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        Content content = serviceResponse.value();

        String redirectUrl = content.getPathname() + "?subscribed=true";

        return ResponseEntity.ok("{\"redirectUrl\": \"" + redirectUrl + "\"}");
    }
}
