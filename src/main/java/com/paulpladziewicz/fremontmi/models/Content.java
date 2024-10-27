package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.*;

@Data
@Document(collection = "content")
public class Content {

    @Id
    private String id;

    private ContentType type;

    private LocalDateTime expiresAt;

    private String pathname;

    private String visibility = ContentVisibility.PUBLIC.getVisibility();

    private String status = ContentStatus.ACTIVE.getStatus();

    private Boolean nearby;

    private Boolean external;

    private ContentDetail detail;

    private List<String> tags = new ArrayList<>();

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

    public void setDetail(ContentType type) {
        switch (type) {
            case GROUP:
                this.detail = new Group();
                break;
            case EVENT:
                this.detail = new Event();
                break;
            case BUSINESS:
                this.detail = new Business();
                break;
            case NEIGHBOR_SERVICES_PROFILE:
                this.detail = new NeighborServicesProfile();
                break;
            default:
                throw new IllegalArgumentException("Unsupported content type: " + type);
        }
    }
}
