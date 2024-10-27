package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
public class Group implements ContentDetail {

    private String title;

    private String description;

    private List<Announcement> announcements = new ArrayList<>();

    private List<String> members = new ArrayList<>();

    @Override
    public void update(Content content, ContentDto contentDto) {
        if (!(contentDto instanceof GroupDto updatedGroup)) {
            throw new IllegalArgumentException("ContentDto is not a GroupDto");
        }
        this.setTitle(updatedGroup.getTitle());
        this.setDescription(updatedGroup.getDescription());
    }
}
