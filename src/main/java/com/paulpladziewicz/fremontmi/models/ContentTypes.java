package com.paulpladziewicz.fremontmi.models;

public enum ContentTypes {
    GROUP("group"),
    EVENT("event");

    private final String contentType;

    ContentTypes(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
