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
    public void update(Content content, ContentDto contentDto) {
        if (!(contentDto instanceof BusinessDto business)) {
            throw new IllegalArgumentException("Invalid content detail type for Business.");
        }

        this.setTitle(business.getTitle());
        this.setHeadline(business.getHeadline());
        this.setDescription(business.getDescription());
        this.setAddress(business.getAddress());
        this.setPhoneNumber(business.getPhoneNumber());
        this.setEmail(business.getEmail());
        this.setWebsite(business.getWebsite());
        this.setDisplayEmail(business.isDisplayEmail());
    }
}