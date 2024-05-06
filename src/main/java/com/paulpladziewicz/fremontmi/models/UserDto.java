package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "users")
public class UserDto {
    @Id
    private String id;

    private String username;

    private String password;

    private String resetPasswordToken;
}
