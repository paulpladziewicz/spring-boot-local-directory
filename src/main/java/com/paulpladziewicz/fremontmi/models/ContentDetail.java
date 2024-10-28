package com.paulpladziewicz.fremontmi.models;

public interface ContentDetail {

    String getTitle();

    default String getEmail() {
        return null;
    }

    void update(Content content, ContentDto updatedContent);
}
