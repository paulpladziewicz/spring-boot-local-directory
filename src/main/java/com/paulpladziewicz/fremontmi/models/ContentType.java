package com.paulpladziewicz.fremontmi.models;

public enum ContentType {
    GROUP("group"),
    EVENT("event"),
    BUSINESS("business"),
    NEIGHBOR_SERVICES_PROFILE("neighbor-services-profile"),
    ARTICLE("article");

    private final String contentType;

    ContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
