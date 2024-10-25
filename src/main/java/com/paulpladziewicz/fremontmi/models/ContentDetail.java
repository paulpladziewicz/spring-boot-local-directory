package com.paulpladziewicz.fremontmi.models;

import java.util.List;
import java.util.Map;

public interface ContentDetail<T extends ContentDetail<T>> {
    String getName();
    List<String> getTags();
    void update(Content<T> parentContent, T newDetail);
    void update(UpdateType updateType, Map<String, Object> updateData);
}
