package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.SecurityContext;
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
            throw new UsernameNotFoundException("UserDto not found");
        }

        return new SecurityContext(userFromDb.getUserId(), userFromDb.getUsername(), userFromDb.getPassword(), userFromDb.isEnabled(), userFromDb.isAccountNonExpired(), userFromDb.isCredentialsNonExpired(), userFromDb.isAccountNonLocked(), userFromDb.getAuthorities());
    }
}
