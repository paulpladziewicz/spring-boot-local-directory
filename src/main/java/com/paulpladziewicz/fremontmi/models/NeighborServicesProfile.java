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

    @NotBlank(message = "Display name is required.")
    @Size(max = 100, message = "Display name should be less than 100 characters.")
    private String title;

    @NotBlank(message = "Description should not be blank.")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format.")
    @Size(max = 100, message = "Email must not be longer than 100 characters.")
    private String email;

    private List<NeighborService> neighborServices = new ArrayList<>();

    private String profileImageUrl;

    private String profileImageFileName;

    @Override
    public void update(Content parentContent, ContentDto newDetail) {
    }
}