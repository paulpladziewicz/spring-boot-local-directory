package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import org.springframework.data.annotation.TypeAlias;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@TypeAlias("Business")
public class Business implements ContentDetail {

    @NotBlank(message = "Event name must not be null")
    @Size(min = 3, max = 100, message = "Event name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Headline cannot be left blank.")
    @Size(min = 3, max = 500, message = "Headline must be between 3 and 100 characters")
    private String headline;

    @NotBlank(message = "Description cannot be left blank.")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    @NotBlank(message = "Please provide an address")
    private String address;
    private boolean displayAddress = false;

    @NotBlank(message = "Please provide a phone number")
    private String phoneNumber;
    private boolean displayPhoneNumber = false;

    @NotBlank(message = "Please provide an email")
    private String email;
    private boolean displayEmail = false;

    private String website;

    private List<String> tags = new ArrayList<>();

    private Boolean nearby;

    private Boolean external;

    @Override
    public void update(Content parentContent, ContentDetail newDetail) {
        if (!(newDetail instanceof Business newBusinessDetail)) {
            throw new IllegalArgumentException("Invalid content detail type for Business.");
        }

        this.setName(newBusinessDetail.getName());
        this.setHeadline(newBusinessDetail.getHeadline());
        this.setDescription(newBusinessDetail.getDescription());

        if (newBusinessDetail.getTags() != null && !newBusinessDetail.getTags().isEmpty()) {
            this.setTags(newBusinessDetail.getTags());
        }

        this.setAddress(newBusinessDetail.getAddress());
        this.setPhoneNumber(newBusinessDetail.getPhoneNumber());
        this.setEmail(newBusinessDetail.getEmail());
        this.setWebsite(newBusinessDetail.getWebsite());
        this.setDisplayEmail(newBusinessDetail.isDisplayEmail());
    }

    @Override
    public void update(UpdateType updateType, Map<String, Object> updateData) {
    }
}