package com.paulpladziewicz.fremontmi.models;

public interface ContentDetail {
    String getTitle();
    void update(Content content, ContentDto updatedContent);
}
