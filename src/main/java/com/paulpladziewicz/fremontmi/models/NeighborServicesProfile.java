package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@TypeAlias("NeighborServicesProfile")
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