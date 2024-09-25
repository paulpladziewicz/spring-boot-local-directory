package com.paulpladziewicz.fremontmi.config;

import com.paulpladziewicz.fremontmi.repositories.UserRepository;
import com.paulpladziewicz.fremontmi.models.UserRecord;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    private final UserRepository userRepository;

    public CustomAuthenticationFailureHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        requestCache.removeRequest(request, response);

        String username = request.getParameter("username");
        Optional<UserRecord> user = userRepository.findByUsername(username);

        if (user.isPresent()) {
            int failedAttempts = user.get().getFailedLoginAttempts() + 1;
            user.get().setFailedLoginAttempts(failedAttempts);

            if (failedAttempts > 3) {
                user.get().setAccountNonLocked(false);
                user.get().setLockTime(LocalDateTime.now());
            }

            userRepository.save(user.get());
        }

        setDefaultFailureUrl(switch (exception.getMessage().toLowerCase()) {
            case "user is disabled" -> "/login?error=notConfirmed";
            case "your account is locked. please try again after 15 minutes." -> "/login?error=locked";
            case "account expired" -> "/login?error=expired";
            case "credentials expired" -> "/login?error=credentialsExpired";
            case "bad credentials" -> "/login?error=badCredentials";
            case "too many failed login attempts" -> "/login?error=tooManyAttempts";
            case "account disabled due to security concerns" -> "/login?error=disabledSecurity";
            default -> "/login?error=true";
        });

        super.onAuthenticationFailure(request, response, exception);
    }
}