package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

@Data
public class PaymentRequest {
    private String id;
    private String paymentIntentId;
    private String paymentStatus;
}
