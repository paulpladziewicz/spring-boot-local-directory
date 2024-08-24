package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

@Data
public class Announcement {

    private int id;

    @NotBlank(message = "Title is required.")
    @Size(max = 256, message = "Title should be less than 256 characters.")
    private String title;

    @NotBlank(message = "Content is required.")
    @Size(max = 2000, message = "Content should be less than 2000 characters.")
    private String content;

    private Date creationDate = new Date();
}
