package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

@Data
public class Business implements ContentDetail {

    private String title;

    private String headline;

    private String description;

    private String address;
    private boolean displayAddress = false;

    private String phoneNumber;
    private boolean displayPhoneNumber = false;

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