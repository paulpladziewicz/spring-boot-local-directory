package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "content")
public class Content {
    @Id
    private String id;

    private String type;

    private String slug; // unique index with type & slug

    private String status;

    private String visibility;

    private List<String> tags = new ArrayList<>();

    private List<String> relatedContentIds;

    private ContentDetails details;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    @Version
    private Long version;
}
