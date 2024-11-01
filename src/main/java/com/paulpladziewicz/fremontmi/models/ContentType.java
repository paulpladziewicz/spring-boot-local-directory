package com.paulpladziewicz.fremontmi.models;

public enum ContentType {
    GROUP,
    EVENT,
    BUSINESS,
    NEIGHBOR_SERVICES_PROFILE;

    public String toHyphenatedString() {
        return this.name().toLowerCase().replace('_', '-');
    }
}