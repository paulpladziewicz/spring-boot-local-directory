package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

@Data
public class InvoiceDTO {
    private String id;
    private String customerId;
    private String status;
    private Long amountDue;
    private Long amountPaid;
    private Long amountRemaining;
    private Long created;
    private String currency;
    private String paymentIntent;
}