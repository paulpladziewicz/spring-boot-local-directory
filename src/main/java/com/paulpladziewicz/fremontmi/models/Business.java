package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=true)
@TypeAlias("Business")
public class Business extends Content {

    @NotBlank(message = "Event name must not be null")
    @Size(min = 3, max = 100, message = "Event name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Headline cannot be left blank.")
    @Size(min = 3, max = 500, message = "Headline must be between 3 and 100 characters")
    private String headline;

    @NotBlank(message = "Description cannot be left blank.")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    private List<String> tags = new ArrayList<>();

    private List<String> administrators = new ArrayList<>();

    private String address;

    private String phoneNumber;

    private String email;

    private boolean displayEmail;

    private String website;
}