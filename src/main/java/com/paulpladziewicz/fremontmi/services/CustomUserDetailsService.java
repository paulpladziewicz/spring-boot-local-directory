package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.CustomUserDetails;
import com.paulpladziewicz.fremontmi.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
                    logger.info("User found and successfully authenticated: {}", username);
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
