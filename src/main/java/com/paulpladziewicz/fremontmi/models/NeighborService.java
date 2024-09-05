package com.paulpladziewicz.fremontmi.models;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "businesses")
public class NeighborService {

    @Id
    private Long id;

    private String name;

    private String description;

    private String category;

    private String address;

    private String phoneNumber;

    private String email;

    private String website;
}