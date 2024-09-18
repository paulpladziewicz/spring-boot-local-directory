package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=true)
@TypeAlias("NeighborServicesProfile")
public class NeighborServicesProfile extends Content {

    @NotBlank(message = "Display name is required.")
    @Size(max = 100, message = "Display name should be less than 100 characters.")
    private String name;

    @NotBlank(message = "Description should not be blank.")
    @Size(max = 5000, message = "Description can't be longer than 5000 characters")
    private String description;

    private List<String> tags = new ArrayList<>();

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format.")
    @Size(max = 100, message = "Email must not be longer than 100 characters.")
    private String email;

    private List<NeighborService> neighborServices = new ArrayList<>();

    private String subscriptionId;

    private String clientSecret;

    private String priceId;
}