package com.paulpladziewicz.fremontmi.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactFormRequest {
    private String slug;
    private String name;
    private String email;
    private String message;
}