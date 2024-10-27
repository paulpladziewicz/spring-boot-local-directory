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
    public void update(Content parentContent, ContentDto newDetail) {
    }
}