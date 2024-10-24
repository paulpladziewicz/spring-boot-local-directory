package com.paulpladziewicz.fremontmi.models;

import lombok.Getter;

@Getter
public enum ContentStatus {
    ACTIVE("active"),
    REQUIRES_ACTIVE_SUBSCRIPTION("requires_active_subscription"),
    PAYMENT_FAILED("payment_failed"),
    DELETED("deleted");

    private final String status;

    ContentStatus(String status) {
        this.status = status;
    }
}
