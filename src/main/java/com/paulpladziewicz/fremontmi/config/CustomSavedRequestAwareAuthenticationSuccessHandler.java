package com.paulpladziewicz.fremontmi.config;

import com.paulpladziewicz.fremontmi.models.UserRecord;
import com.paulpladziewicz.fremontmi.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Optional;

public class CustomSavedRequestAwareAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    public CustomSavedRequestAwareAuthenticationSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        Optional<UserRecord> user = userRepository.findByUsername(username);

        if (user.isPresent()) {
            UserRecord userRecord = user.get();
            if (userRecord.getFailedLoginAttempts() > 0) {
                userRecord.setFailedLoginAttempts(0);
                userRepository.save(userRecord);
            }
        }

        SavedRequest savedRequest = requestCache.getRequest(request, response);

        if (savedRequest == null) {
            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, getDefaultTargetUrl());
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}