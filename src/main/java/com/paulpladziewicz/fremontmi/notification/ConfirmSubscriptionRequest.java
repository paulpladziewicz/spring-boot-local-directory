package com.paulpladziewicz.fremontmi.notification;

import lombok.Data;

@Data
public class ConfirmSubscriptionRequest {
    private String contentId;
    private String contentType;
    private String subscriptionId;
    private String priceId;
    private String displayName;
    private String displayPrice;
    private String paymentIntentId;
    private String paymentStatus;
}
