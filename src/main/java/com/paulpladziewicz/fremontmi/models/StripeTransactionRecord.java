package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "stripe_transaction_records")
public class StripeTransactionRecord {

    @Id
    private String id;

    private String status = "incomplete";

    private String customerId;

    private String subscriptionId;

    private String priceId;

    private String displayName;

    private String displayPrice;

    private String paymentIntentId;

    private String clientSecret;

    private String entityId;

    private String entityCollection;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
