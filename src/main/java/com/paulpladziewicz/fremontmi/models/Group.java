package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "groups")
public class Group {

    @Id
    private String id;

    @NotNull(message = "Group name must not be null")
    @Size(min = 3, max = 100, message = "Group name must be between 3 and 100 characters")
    private String name;

    @NotNull(message = "Description must not be null")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    private String imageUrl;

    private List<String> tags;

    private String status = "active";

    private String visibility = "public";

    private String joinPolicy = "open";

    private List<String> administrators = new ArrayList<>();

    private List<String> members = new ArrayList<>();

    private List<Announcement> announcements = new ArrayList<>();

    private Date creationDate = new Date();
}
