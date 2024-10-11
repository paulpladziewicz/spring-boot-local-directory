package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "billing_info")
public class BillingInfo {
    @Id
    private String id;
    private String customerId;
    private String contentId;
    private String subscriptionId;
    private String invoiceId;
    private Date billingDate;
    private Double amount;
    private String status;
}
