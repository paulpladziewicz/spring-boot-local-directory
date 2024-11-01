package com.paulpladziewicz.fremontmi.models;

import java.util.List;

public interface ContentDto {
    boolean isNearby();
    boolean isExternal();
    String getContentId();
    String getTitle();
    List<String> getTags();
}
