package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "content")
public class Content {
    @Id
    private String id;

    private String type; // enum GROUP, EVENT, ANNOUNCEMENT, RESOURCE, USER, etc.

    private String pathname;

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

    // Stripe related fields

    private Map<String, Object> stripeDetails;

    private String price;

    private String planName;

    private String subscriptionId;

    private String clientSecret;

    private String priceId;
}
