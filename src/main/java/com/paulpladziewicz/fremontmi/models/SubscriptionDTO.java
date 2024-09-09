package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

import java.util.List;

@Data
public class SubscriptionDTO {

    private String id;  // Subscription ID
    private String customerId;  // Customer ID
    private String status;  // Subscription status (active, canceled, etc.)
    private String collectionMethod;  // Charge automatically or send invoice
    private String currency;  // Currency (e.g., USD)
    private Integer amount;  // Amount in smallest currency unit (e.g., cents for USD)
    private String planName;  // Name of the plan or product
    private String interval;  // Billing interval (e.g., month, year)
    private Integer intervalCount;  // Interval count (e.g., every 1 month)
    private Long startDate;  // Unix timestamp for when the subscription started
    private Long currentPeriodStart;  // Unix timestamp for current billing period start
    private Long currentPeriodEnd;  // Unix timestamp for current billing period end
    private Boolean cancelAtPeriodEnd;  // If the subscription will cancel at period end
    private Long canceledAt;  // Unix timestamp when subscription was canceled
    private String latestInvoice;  // Latest invoice ID
    private List<String> paymentMethods;  // List of payment methods (e.g., "card")

    // Getters and setters...
}
