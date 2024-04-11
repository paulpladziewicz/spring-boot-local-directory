package com.paulpladziewicz.fremontmi.models;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

public class CustomUser extends User {

    private String id;

    private String firstName;

    private String lastName;

    public CustomUser(String username, String password) {
        super(username, password, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}

