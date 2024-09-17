package com.paulpladziewicz.fremontmi.models;

public enum ContentStatus {
    REQUIRES_ACTIVE_SUBSCRIPTION("requires_active_subscription"),
    DELETED("deleted");

    private final String status;

    ContentStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
