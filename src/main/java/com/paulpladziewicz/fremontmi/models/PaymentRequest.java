package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

@Data
public class PaymentRequest {
    private String entityId;
    private String paymentIntentId;
    private String paymentStatus;
}
