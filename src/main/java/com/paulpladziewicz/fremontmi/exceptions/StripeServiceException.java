package com.paulpladziewicz.fremontmi.exceptions;

public class StripeServiceException extends RuntimeException {
    public StripeServiceException(String message) {
        super(message);
    }

    public StripeServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}