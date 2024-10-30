package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

import java.time.Instant;

@Data
public class Announcement {

    private int id;

    private String title;

    private String message;

    private Instant createdAt;
}
