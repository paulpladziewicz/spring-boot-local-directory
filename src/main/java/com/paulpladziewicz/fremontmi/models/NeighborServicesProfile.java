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
    private String name;

    @NotBlank(message = "Description should not be blank.")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    private List<String> tags = new ArrayList<>();

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format.")
    @Size(max = 100, message = "Email must not be longer than 100 characters.")
    private String email;

    private List<NeighborService> neighborServices = new ArrayList<>();

    private String profileImageUrl;

    private String profileImageFileName;

    private Boolean nearby;

    private Boolean external;

    @Override
    public void update(Content parentContent, ContentDetail newDetail) {
    }

    @Override
    public void update(UpdateType updateType, Map<String, Object> updateData) {
        switch (updateType) {
            case PROFILE_IMAGE:
                String profileImageUrl = (String) updateData.get("profileImageUrl");
                String profileImageFileName = (String) updateData.get("profileImageFileName");
                this.profileImageUrl = profileImageUrl;
                this.profileImageFileName = profileImageFileName;
                break;
            // Add more cases for other types of updates
            default:
                throw new IllegalArgumentException("Update type not supported: " + updateType);
        }
    }
}