package com.paulpladziewicz.fremontmi.user;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.paulpladziewicz.fremontmi.content.ContentAction;
import com.paulpladziewicz.fremontmi.content.ContentType;

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

    private Map<ContentType, Map<ContentAction, Set<String>>> contentActions = new HashMap<>();

    private int emailSendCount;

    private LocalDateTime termsAcceptedAt;
}
