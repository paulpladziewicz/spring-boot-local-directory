package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.CustomUserDetails;
import com.paulpladziewicz.fremontmi.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(userFromDb -> {
                    if (!userFromDb.isAccountNonLocked()) {
                        if (userFromDb.getLockTime() != null && userFromDb.getLockTime().plusMinutes(15).isBefore(LocalDateTime.now())) {
                            userFromDb.setAccountNonLocked(true);
                            userFromDb.setFailedLoginAttempts(0);
                            userFromDb.setLockTime(null);
                            userRepository.save(userFromDb);
                            logger.info("User account unlocked after lock duration expired: {}", username);
                        } else {
                            logger.warn("User account is currently locked: {}", username);
                            throw new LockedException("Your account is locked. Please try again after 15 minutes.");
                        }
                    }

                    logger.info("User found and attempted login: {}", username);

                    return new CustomUserDetails(
                            userFromDb.getUserId(),
                            userFromDb.getUsername(),
                            userFromDb.getPassword(),
                            userFromDb.isEnabled(),
                            userFromDb.isAccountNonExpired(),
                            userFromDb.isCredentialsNonExpired(),
                            userFromDb.isAccountNonLocked(),
                            userFromDb.getAuthorities()
                    );
                })
                .orElseThrow(() -> {
                    logger.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });
    }
}
