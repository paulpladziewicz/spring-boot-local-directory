package com.paulpladziewicz.fremontmi.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.ui.Model;

import java.io.IOException;

public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        // Log the exception message
        System.out.println("Authentication failed: " + exception.getMessage());

        // Clear any saved request from the cache to prevent `continue` parameter issues
        requestCache.removeRequest(request, response);

        // Redirect based on the type of failure
        if ("User is disabled".equalsIgnoreCase(exception.getMessage())) {
            setDefaultFailureUrl("/login?error=notConfirmed");
        } else {
            setDefaultFailureUrl("/login?error=true");
        }

        // Proceed with the failure handling
        super.onAuthenticationFailure(request, response, exception);
    }
}