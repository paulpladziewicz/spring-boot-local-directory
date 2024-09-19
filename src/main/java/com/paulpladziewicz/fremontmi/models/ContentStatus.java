package com.paulpladziewicz.fremontmi.models;

public enum ContentStatus {
    ACTIVE("active"),
    REQUIRES_ACTIVE_SUBSCRIPTION("requires_active_subscription"),
    PAYMENT_FAILED("payment_failed"),
    DELETED("deleted");

    private final String status;

    ContentStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
