package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

@Data
public class TagUsage {
    private String id;  // The tag name (as MongoDB groups by "_id")
    private int count;  // The number of times the tag is used
}