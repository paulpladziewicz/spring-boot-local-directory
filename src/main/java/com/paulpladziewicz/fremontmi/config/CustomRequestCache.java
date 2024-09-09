package com.paulpladziewicz.fremontmi.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

public class CustomRequestCache extends HttpSessionRequestCache {

    @Override
    public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
        // Get the request URI
        String requestURI = request.getRequestURI();
        // Get query string parameters
        String queryString = request.getQueryString();

        // Debugging log to see the URI and query string
        System.out.println("Request URI: " + requestURI);
        System.out.println("Query String: " + queryString);

        // Avoid saving the request for login or login error pages
        if (requestURI.equals("/login") || (queryString != null && queryString.contains("error=true"))) {
            System.out.println("Skipping request cache for: " + requestURI + "?" + queryString);
            return;
        }

        // For all other requests, use the default behavior
        super.saveRequest(request, response);
    }
}