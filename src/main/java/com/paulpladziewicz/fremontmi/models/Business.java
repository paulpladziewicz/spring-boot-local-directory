package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Business implements ContentDetail {

    @NotBlank(message = "Event name must not be null")
    @Size(min = 3, max = 100, message = "Event name must be between 3 and 100 characters")
    private String title;

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

    @Override
    public void update(Content parentContent, ContentDto newDetail) {
        if (!(newDetail instanceof Business newBusinessDetail)) {
            throw new IllegalArgumentException("Invalid content detail type for Business.");
        }

        this.setTitle(newBusinessDetail.getTitle());
        this.setHeadline(newBusinessDetail.getHeadline());
        this.setDescription(newBusinessDetail.getDescription());
        this.setAddress(newBusinessDetail.getAddress());
        this.setPhoneNumber(newBusinessDetail.getPhoneNumber());
        this.setEmail(newBusinessDetail.getEmail());
        this.setWebsite(newBusinessDetail.getWebsite());
        this.setDisplayEmail(newBusinessDetail.isDisplayEmail());
    }
}