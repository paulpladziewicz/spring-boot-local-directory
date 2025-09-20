package com.paulpladziewicz.fremontmi.app.exceptions;

public class UserProfileNotFoundException extends RuntimeException {
    public UserProfileNotFoundException(String message) {
        super(message);
    }
}
