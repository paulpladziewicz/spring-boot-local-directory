package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.*;

@Data
@Document(collection = "content")
public class Content <T extends ContentDetail<T>> {
    @Id
    private String id;

    private ContentType type;

    private Boolean nearby;

    private Boolean external;

    private LocalDateTime expiresAt;

    private String pathname;

    private String visibility = ContentVisibility.PUBLIC.getVisibility();

    private String status = ContentStatus.ACTIVE.getStatus();

    private T detail;

    private List<String> administrators = new ArrayList<>();

    private int heartCount = 0;

    private Set<String> heartedUserIds = new HashSet<>();

    private List<String> relatedContentIds;

    private String parentContentId;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private Boolean reviewed = false;

    @Version
    private Long version;
}
