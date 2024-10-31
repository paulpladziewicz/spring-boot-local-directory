package com.paulpladziewicz.fremontmi.models;

import java.util.List;

public interface ContentDto {
    boolean isNearby();
    String getTitle();
    List<String> getTags();
}
