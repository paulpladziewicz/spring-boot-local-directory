package com.paulpladziewicz.fremontmi.app.exceptions;

public class StripeServiceException extends RuntimeException {
    public StripeServiceException(String message) {
        super(message);
    }

    public StripeServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}