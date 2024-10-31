package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NeighborServicesProfile implements ContentDetail {

    private String title;

    private String description;

    private String email;

    private List<NeighborService> neighborServices = new ArrayList<>();

    private String profileImageUrl;

    private String profileImageFileName;

    @Override
    public void update(Content content, ContentDto contentDto) {
        if (!(contentDto instanceof NeighborServicesProfileDto profile)) {
            throw new IllegalArgumentException("Invalid content detail type for Business.");
        }

        this.title = profile.getTitle();
        this.description = profile.getDescription();
        this.email = profile.getEmail();
        this.neighborServices = profile.getNeighborServices();
    }
}