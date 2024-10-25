package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class Group implements ContentDetail {

    public Group() {
        // Default constructor required for MongoDB deserialization
    }

    @NotBlank(message = "Please provide a group name.")
    @Size(max = 100, message = "Name should not be longer than 100 characters.")
    private String name;

    @NotBlank(message = "Please provide a group description.")
    @Size(max = 5000, message = "Description should not be longer than 3,000 characters, which is about 5000 words.")
    private String description;

    private List<String> tags = new ArrayList<>();

    private List<Announcement> announcements = new ArrayList<>();

    private List<String> members = new ArrayList<>();

    private Boolean nearby;

    private Boolean external;

    @Override
    public void update(Content parentContent, ContentDetail newDetail) {
        if (!(newDetail instanceof Business newGroupDetail)) {
            throw new IllegalArgumentException("Invalid content detail type for Business.");
        }
        this.setName(newGroupDetail.getName());
        this.setDescription(newGroupDetail.getDescription());
    }

    @Override
    public void update(UpdateType updateType, Map<String, Object> updateData) {
    }
}
