package com.paulpladziewicz.fremontmi.models;

import java.util.List;
import java.util.Map;

public interface ContentDetail {
    String getName();
    List<String> getTags();
    void setTags(List<String> validatedTags);
    void update(Content parentContent, ContentDetail newDetail);
    void update(UpdateType updateType, Map<String, Object> updateData);
}
