package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GroupDto implements ContentDto {
    private String contentId;

    private String pathname;

    private List<String> tags = new ArrayList<>();

    @NotBlank(message = "Please provide a group name.")
    @Size(max = 100, message = "Name should not be longer than 100 characters.")
    private String title;

    @NotBlank(message = "Please provide a group description.")
    @Size(max = 5000, message = "Description should not be longer than 3,000 characters, which is about 5000 words.")
    private String description;
}
