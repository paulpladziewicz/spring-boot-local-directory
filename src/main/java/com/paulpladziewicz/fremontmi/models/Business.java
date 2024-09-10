package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "businesses")
public class Business {

    @Id
    private String id;

    @NotBlank(message = "Business name cannot be left blank.")
    @Size(min = 3, max = 256, message = "Business name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Headline cannot be left blank.")
    @Size(min = 3, max = 500, message = "Headline must be between 3 and 100 characters")
    private String headline;

    @NotBlank(message = "Description cannot be left blank.")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    private List<String> categories =  new ArrayList<>();

    private List<String> tags = new ArrayList<>();

    private List<String> adminIds = new ArrayList<>();

    private String address;

    private String phoneNumber;

    private String email;

    private String website;

    private String status = "incomplete";

    private String clientSecret;

    private String subscriptionPriceId;

    private String stripeSubscriptionId;

    private String paymentIntentId;

    private String paymentStatus = "incomplete";

    private LocalDateTime subscriptionEndTime;
}