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

    @NotBlank(message = "Event name must not be null")
    @Size(min = 3, max = 100, message = "Event name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Description must not be null")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    private List<String> categories =  new ArrayList<>();

    private List<String> tags = new ArrayList<>();

    private String address;

    private String phoneNumber;

    private String email;

    private String website;

    private String status;

    private String clientSecret;

    private String subscriptionPriceId;

    private String stripeSubscriptionId;

    private String paymentIntentId;

    private String paymentStatus;

    private LocalDateTime subscriptionEndTime;
}