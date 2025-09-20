package com.paulpladziewicz.fremontmi.content;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class NeighborServicesProfile implements ContentDetail {

    private String title;

    private String description;

    private String email;

    private List<NeighborService> neighborServices = new ArrayList<>();

    private String profileImageUrl;

    private String profileImageFileName;

    private Map<String, Object> images;

    private String externalUrl;

    private Map<String, String> socialLinks;

    @Override
    public void update(Content content, ContentDto contentDto) {
        if (!(contentDto instanceof NeighborServicesProfileDto profile)) {
            throw new IllegalArgumentException("Invalid content detail type for Business.");
        }

        setTitle(profile.getTitle());
        setDescription(profile.getDescription());
        setEmail(profile.getEmail());
        setNeighborServices(profile.getNeighborServices());
    }
}