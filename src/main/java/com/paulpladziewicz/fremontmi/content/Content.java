package com.paulpladziewicz.fremontmi.content;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.LocalDateTime;
import java.util.*;

@Data
@Document(collection = "content")
@CompoundIndexes({
    @CompoundIndex(name = "event_start_time_idx", def = "{'detail.days.startTime': 1}")
})
public class Content {

    @Id
    private String id;

    private ContentType type;

    @Indexed
    private String pathname;

    private ContentVisibility visibility = ContentVisibility.PUBLIC;

    private ContentStatus status = ContentStatus.ACTIVE;

    private boolean nearby;

    private boolean external;

    private ContentDetail detail;

    private List<String> tags = new ArrayList<>();

    private List<String> relatedContentIds;

    private Set<String> participants = new HashSet<>();

    private Set<String> administrators = new HashSet<>();

    private int heartCount = 0;

    private Set<String> heartedUserIds = new HashSet<>();

    private String parentContentId;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private boolean reviewed = false;

    @Version
    private Long version;

    public void setDetail(ContentType type) {
        this.detail = switch (type) {
            case GROUP -> new Group();
            case EVENT -> new Event();
            case BUSINESS -> new Business();
            case NEIGHBOR_SERVICES_PROFILE -> new NeighborServicesProfile();
        };
    }
}
