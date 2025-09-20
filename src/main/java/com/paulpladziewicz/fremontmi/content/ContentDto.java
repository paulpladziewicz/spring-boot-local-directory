package com.paulpladziewicz.fremontmi.content;

import java.util.List;

public interface ContentDto {
    boolean isNearby();
    boolean isExternal();
    String getContentId();
    String getTitle();
    List<String> getTags();
}
