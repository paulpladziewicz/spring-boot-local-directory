package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "user_details")
public class UserDetailsDto {
    @Id
    private String userId;

    private String firstName;

    private String lastName;

    private String email;

    private List<String> groupIds = new ArrayList<>();

    private List<String> groupAdminIds = new ArrayList<>();

    private List<String> eventAdminIds = new ArrayList<>();

    private LocalDateTime termsAcceptedAt;
}
