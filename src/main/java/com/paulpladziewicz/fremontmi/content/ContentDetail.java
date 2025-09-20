package com.paulpladziewicz.fremontmi.content;

public interface ContentDetail {

    String getTitle();

    String getDescription();

    default String getEmail() {
        return null;
    }

    void update(Content content, ContentDto updatedContent);
}
