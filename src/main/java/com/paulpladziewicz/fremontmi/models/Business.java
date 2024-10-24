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
public class Business implements ContentDetail<Business> {

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

    @Override
    public void update(Content<Business> parentContent, Business newDetail) {
        Business existingDetail = parentContent.getDetail();
        existingDetail.setName(newDetail.getName());
        existingDetail.setHeadline(newDetail.getHeadline());
        existingDetail.setDescription(newDetail.getDescription());

        if (newDetail.getTags() != null && !newDetail.getTags().isEmpty()) {
            existingDetail.setTags(newDetail.getTags());
        }

        existingDetail.setAddress(newDetail.getAddress());
        existingDetail.setPhoneNumber(newDetail.getPhoneNumber());
        existingDetail.setEmail(newDetail.getEmail());
        existingDetail.setWebsite(newDetail.getWebsite());
        existingDetail.setDisplayEmail(newDetail.isDisplayEmail());
    }

    @Override
    public void update(UpdateType updateType, Map<String, Object> updateData) {
    }
}