package com.paulpladziewicz.fremontmi.app.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.paulpladziewicz.fremontmi.app.exceptions.StripeServiceException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private HttpServletRequest request;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> ((FieldError) error).getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        if (isApiCall()) {
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        } else {
            request.setAttribute("errors", errors);
            return "error";
        }
    }

    @ExceptionHandler(DataAccessException.class)
    public Object handleDataAccessException(DataAccessException e) {
        String errorMessage = "A database error occurred. Please try again later.";

        if (isApiCall()) {
            return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            request.setAttribute("errorMessage", errorMessage);
            return "error";
        }
    }

    @ExceptionHandler(StripeServiceException.class)
    public Object handleStripeServiceException(StripeServiceException e) {
        String errorMessage = "An issue occurred with our payment service. Please try again later.";
//        logger.error("StripeServiceException: ", ex);

        if (isApiCall()) {
            return new ResponseEntity<>(errorMessage, HttpStatus.SERVICE_UNAVAILABLE);
        } else {
            request.setAttribute("errorMessage", errorMessage);
            return "error";
        }
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception e) {
        String errorMessage = "An unexpected error occurred. Please try again later.";

        if (isApiCall()) {
            return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            request.setAttribute("errorMessage", errorMessage);
            return "error";
        }
    }

    private boolean isApiCall() {
        String acceptHeader = request.getHeader("Accept");
        String xhrHeader = request.getHeader("X-Requested-With");
        return (acceptHeader != null && acceptHeader.contains("application/json")) ||
                (xhrHeader != null && "XMLHttpRequest".equals(xhrHeader));
    }
}

