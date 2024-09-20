package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

import java.util.List;

@Data
public class SubscriptionDTO {
    private String id;
    private String planName;
    private String price;
    private String nextRecurringPayment;
    private String subscriptionEnd;
    private boolean cancelAtPeriodEnd;
}
