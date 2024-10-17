package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

@Data
public class ConfirmSubscriptionRequest {
    private String contentId;
    private String contentType;
    private String subscriptionId;
    private String priceId;
    private String paymentIntentId;
    private String paymentStatus;
}
