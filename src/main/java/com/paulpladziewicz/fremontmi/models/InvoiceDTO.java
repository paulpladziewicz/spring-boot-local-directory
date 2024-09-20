package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

@Data
public class InvoiceDTO {
    private String id;  // Invoice ID
    private String planName;  // Plan description
    private Long amountPaid;  // Amount paid in smallest currency unit (e.g., cents)
    private String customerName;  // Customer's name
    private String paidDate;  // Paid date as formatted string
    private String invoiceUrl;  // Hosted invoice URL
}