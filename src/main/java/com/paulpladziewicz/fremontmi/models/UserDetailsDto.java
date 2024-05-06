package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "user_details")
public class UserDetailsDto {
    @Id
    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private List<String> groupIds;

    private List<String> groupAdminIds;
}
