package com.paulpladziewicz.fremontmi.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;

@Document(collection = "groups")
public class Group {
    @Id
    private String id;

    @NotNull(message = "Group name must not be null")
    @Size(min = 3, max = 100, message = "Group name must be between 3 and 100 characters")
    private String name;

    @NotNull(message = "Description must not be null")
    @Size(max = 500, message = "Description can't be longer than 500 characters")
    private String description;

    private String imageUrl;

    private List<String> tags;

    private String status = "active";

    private String visibility = "public";

    private String joinPolicy = "open";

    private List<String> administrators;

    private List<String> members;

    private Date creationDate = new Date();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getJoinPolicy() {
        return joinPolicy;
    }

    public void setJoinPolicy(String joinPolicy) {
        this.joinPolicy = joinPolicy;
    }

    public List<String> getAdministrators() {
        return administrators;
    }

    public void setAdministrators(List<String> administrators) {
        this.administrators = administrators;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
