package com.paulpladziewicz.fremontmi.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRegistrationDto {

    @NotBlank(message = "First name is required.")
    @Size(max = 30, message = "First name should be less than 30 characters.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(max = 30, message = "Last name should be less than 30 characters.")
    private String lastName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format.")
    @Size(max = 100, message = "Email must not be longer than 100 characters.")
    private String email;

    @NotBlank(message = "Password is required.")
    private String password;

    @NotBlank(message = "Matching password is required.")
    private String matchingPassword;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMatchingPassword() {
        return matchingPassword;
    }

    public void setMatchingPassword(String matchingPassword) {
        this.matchingPassword = matchingPassword;
    }
}
