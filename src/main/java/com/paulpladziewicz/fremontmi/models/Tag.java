package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Data
@Document(collection = "tags")
public class Tag {
    @Id
    private String id;

    private String name; // canonical form, unique index

    public Tag(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    private String displayName; // user-friendly version

    private int count;

    private Map<String, Integer> countByContentType = new HashMap<>();
}
