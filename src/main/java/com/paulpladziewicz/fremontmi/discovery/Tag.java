package com.paulpladziewicz.fremontmi.discovery;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.paulpladziewicz.fremontmi.content.ContentType;

import java.util.HashMap;
import java.util.Map;

@Data
@Document(collection = "tags")
public class Tag {
    @Id
    private String id;

    private String name;

    private String displayName;

    private Boolean reviewed = false;

    private int count;

    private Map<ContentType, Integer> countByContentType = new HashMap<>();

    public Tag(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public void incrementCountForContentType(ContentType contentType) {
        this.countByContentType.put(contentType, this.countByContentType.getOrDefault(contentType, 0) + 1);
    }

    public void decrementCountForContentType(ContentType contentType) {
        this.countByContentType.computeIfPresent(contentType, (key, count) -> Math.max(count - 1, 0));
    }
}
