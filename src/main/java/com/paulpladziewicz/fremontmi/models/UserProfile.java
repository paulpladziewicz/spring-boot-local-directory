package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.*;

@Data
@Document(collection = "user_profiles")
public class UserProfile {
    @Id
    private String userId;

    private String stripeCustomerId;

    private String firstName;

    private String lastName;

    private String email;

    private Map<ContentType, ContentActions> contentActionsByType = new HashMap<>();

    @Data
    public static class ContentActions {
        private Set<String> created = new HashSet<>();
        private Set<String> bookmarked = new HashSet<>();
        private Set<String> hearted = new HashSet<>();
    }

    private int emailSendCount;

    private LocalDateTime termsAcceptedAt;
}
