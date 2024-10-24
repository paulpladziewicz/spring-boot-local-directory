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

    private List<String> groupIds = new ArrayList<>();

    private List<String> eventIds = new ArrayList<>();

    private List<String> businessIds = new ArrayList<>();

    private Set<String> bookmarkedContentIds = new HashSet<>();

    private int emailSendCount;

    private LocalDateTime termsAcceptedAt;
}
