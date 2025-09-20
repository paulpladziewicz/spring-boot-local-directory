package com.paulpladziewicz.fremontmi.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BusinessDto implements ContentDto {
    private String contentId;

    private String pathname;

    private boolean external;

    private boolean nearby;

    private List<String> tags = new ArrayList<>();

    @NotBlank(message = "Event name must not be null")
    @Size(min = 3, max = 100, message = "Event name must be between 3 and 100 characters")
    private String title;

    @NotBlank(message = "Headline cannot be left blank.")
    @Size(min = 3, max = 500, message = "Headline must be between 3 and 100 characters")
    private String headline;

    @NotBlank(message = "Description cannot be left blank.")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    private String address;
    private boolean displayAddress = false;

    private String phoneNumber;
    private boolean displayPhoneNumber = false;

    private String email;
    private boolean displayEmail = false;

    private String website;
}
