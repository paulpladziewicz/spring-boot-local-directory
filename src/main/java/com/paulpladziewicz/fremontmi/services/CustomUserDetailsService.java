package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.CustomUser;
import com.paulpladziewicz.fremontmi.models.UserDto;
import com.paulpladziewicz.fremontmi.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDto userFromDb = userRepository.findByUsername(username);

        if (userFromDb == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return new CustomUser(userFromDb.getUsername(), userFromDb.getPassword());
    }
}
