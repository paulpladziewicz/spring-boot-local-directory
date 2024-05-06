package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

@Data
public class GroupDetailsDto {
    private Group group;
    private String userRole; // This can be "admin" or "member"
}