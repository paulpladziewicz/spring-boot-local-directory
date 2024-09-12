package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "groups")
public class Group {
    @Id
    private String id;

    @NotBlank(message = "Please provide a group name.")
    @Size(max = 100, message = "Name should not be longer than 100 characters.")
    private String name;

    @NotBlank(message = "Please provide a group description.")
    @Size(max = 5000, message = "Description should not be longer than 3,000 characters, which is about 5000 words.")
    private String description;

    private List<String> tags = new ArrayList<>();

    private String status = "active";

    private List<String> administrators = new ArrayList<>();

    private List<String> members = new ArrayList<>();

    private List<Announcement> announcements = new ArrayList<>();

    private Date creationDate = new Date();
}
