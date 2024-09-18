package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.PaymentRequest;
import com.paulpladziewicz.fremontmi.services.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StripeController {

    private final StripeService stripeService;

    public StripeController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @GetMapping("/pay/subscription")
    public String paySubscription(@RequestParam("contentId") String contentId, Model model) {
        // get content by id from Stripe service and pass to view
        return "pay-subscription";
    }

    @PostMapping("/subscription-payment-success")
    @ResponseBody
    public String subscriptionPaymentSuccess(@RequestBody PaymentRequest paymentRequest) {
        stripeService.handleSubscriptionSuccess(paymentRequest);

        // TODO update this to redirect correctly
        return "redirect:/businesses/" + paymentRequest.getEntityId();
    }
}
