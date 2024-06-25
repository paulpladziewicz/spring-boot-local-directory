package com.paulpladziewicz.fremontmi.services;

import com.stripe.Stripe;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Subscription;
import com.stripe.param.ChargeCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.SubscriptionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

//@Service
public class StripeService {

    @Value("${stripe.apiKey}")
    private String apiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }

    public Charge chargeCreditCard(String token, double amount) throws Exception {
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", (int)(amount * 100)); // amount in cents
        chargeParams.put("currency", "usd");
        chargeParams.put("source", token);
        chargeParams.put("description", "Charge for product or service");

        ChargeCreateParams params = ChargeCreateParams.builder()
//                .putAll(chargeParams)
                .build();

        return Charge.create(params);
    }

    public Subscription createAnnualSubscription(String paymentMethodId, String email) throws Exception {
        // Create a customer
        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setEmail(email)
                .setPaymentMethod(paymentMethodId)
                .setInvoiceSettings(CustomerCreateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(paymentMethodId)
                        .build())
                .build();

        Customer customer = Customer.create(customerParams);

        // Attach the payment method to the customer
        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
        paymentMethod.attach(PaymentMethodAttachParams.builder()
                .setCustomer(customer.getId())
                .build());

        // Create subscription
        SubscriptionCreateParams subscriptionParams = SubscriptionCreateParams.builder()
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice("price_ID") // Replace with the actual price ID
                        .build())
                .setCustomer(customer.getId())
                .setOffSession(true) // Setting to true because it's a repeat customer scenario
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
//                .setExpand(Arrays.asList("latest_invoice.payment_intent"))
                .build();

        return Subscription.create(subscriptionParams);
    }
}