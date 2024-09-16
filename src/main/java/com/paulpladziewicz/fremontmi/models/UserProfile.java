package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private String neighborServiceProfileId;

    Map<String, List<String>> favoritedContent = new HashMap<>();

    private LocalDateTime termsAcceptedAt;

    // remove
    private List<String> groupAdminIds = new ArrayList<>();

    // remove
    private List<String> eventAdminIds = new ArrayList<>();
}
