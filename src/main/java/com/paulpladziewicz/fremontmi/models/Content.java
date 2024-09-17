package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// One Service for each type of content to handle all the nuances of that type
@Data
@Document(collection = "content")
public class Content {
    @Id
    private String id;

    private String slug; // unique index with type & slug

    private String visibility = ContentVisibility.PUBLIC.getVisibility(); // enum PUBLIC, RESTRICTED, HIDDEN

    private String status = ContentStatus.ACTIVE.getStatus(); // enum REQUIRES_ACTIVE_SUBSCRIPTION, DELETED

    private List<String> relatedContentIds; // future use

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt; // user made changes

    private Boolean reviewed = false;

    @Version
    private Long version;
}
