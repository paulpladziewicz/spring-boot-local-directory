package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

@Data
public class EventDetailsDto {
    private Event event;
    private String userRole; // This can be "admin" or "member"
}