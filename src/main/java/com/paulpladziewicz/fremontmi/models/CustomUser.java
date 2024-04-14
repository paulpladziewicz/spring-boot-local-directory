package com.paulpladziewicz.fremontmi.models;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

public class CustomUser extends User {

    public CustomUser(String username, String password) {
        super(username, password, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }
}

