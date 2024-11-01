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

        setTitle(business.getTitle());
        setHeadline(business.getHeadline());
        setDescription(business.getDescription());
        setAddress(business.getAddress());
        setPhoneNumber(business.getPhoneNumber());
        setEmail(business.getEmail());
        setWebsite(business.getWebsite());
        setDisplayEmail(business.isDisplayEmail());
    }
}