package com.paulpladziewicz.fremontmi.content;

import lombok.Data;

import java.util.Map;

@Data
public class Business implements ContentDetail {

    private String title;

    private String headline;

    private String description;

    private String address;
    private boolean displayAddress = true;

    private String phoneNumber;
    private boolean displayPhoneNumber = true;

    private String email;
    private boolean displayEmail = false;

    private String website;

    private Map<String, String> socialLinks;

    private Map<String, String> businessHours;
    private Map<String, String> seasonalHours;
    private Map<String, String> holidayHours;

    private Map<String, Object> images;

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