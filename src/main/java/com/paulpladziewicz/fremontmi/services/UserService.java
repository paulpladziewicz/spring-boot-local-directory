package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.User;
import com.paulpladziewicz.fremontmi.models.UserRegistrationDto;
import com.paulpladziewicz.fremontmi.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerNewUserAccount(UserRegistrationDto registrationDto) {
//        if (userRepository.findByUsername(registrationDto.getEmail()) != null) {
//            throw new RuntimeException("There is an account with that email address: " + registrationDto.getEmail());
//        }
        User user = new User();
        user.setUsername(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        logger.info("About to call userRepository {} {}", user.getUsername(), user.getPassword());

        userRepository.save(user);
    }
}
