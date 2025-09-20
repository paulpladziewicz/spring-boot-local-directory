package com.paulpladziewicz.fremontmi.billing;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "stripe_transaction_records")
public class StripeSubscriptionRecord {

    @Id
    private String id;

    private String contentId;

    private String contentType;

    private String userId;

    private String stripeCustomerId;

    private String subscriptionId;

    private String priceId;

    private String displayName;

    private String displayPrice;

    private String status;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
