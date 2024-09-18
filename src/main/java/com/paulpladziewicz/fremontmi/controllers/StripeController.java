package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.PaymentRequest;
import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.services.StripeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class StripeController {

    private final StripeService stripeService;

    public StripeController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @GetMapping("/pay/subscription")
    public String paySubscription(@RequestParam("contentId") String contentId, Model model) {
        // get content by id from Stripe service and pass to view
        return "stripe/pay-subscription";
    }

    @GetMapping("/api/get-content/{contentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getContent(@PathVariable String contentId) {
        ServiceResponse<Map<String, Object>> serviceResponse = stripeService.getContentDetails(contentId);

        if (serviceResponse.hasError()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok(serviceResponse.value());
    }

    @PostMapping("/subscription-payment-success")
    @ResponseBody
    public ResponseEntity<String> subscriptionPaymentSuccess(@RequestBody PaymentRequest paymentRequest) {
        stripeService.handleSubscriptionSuccess(paymentRequest);

        // Redirect to content page
        return ResponseEntity.ok("{\"redirectUrl\": \"/businesses/" + paymentRequest.getEntityId() + "\"}");
    }
}
