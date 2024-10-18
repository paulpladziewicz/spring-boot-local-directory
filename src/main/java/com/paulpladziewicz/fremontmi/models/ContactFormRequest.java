package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactFormRequest {

    private String id;

    private String slug;

    @NotBlank
    private String name;

    @Email
    private String email;

    @NotBlank
    private String message;
}