package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.CustomUser;
import com.paulpladziewicz.fremontmi.models.UserDto;
import com.paulpladziewicz.fremontmi.models.UserRegistrationDto;
import com.paulpladziewicz.fremontmi.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDto userFromDb = userRepository.findByUsername(username);

        if (userFromDb == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return new CustomUser(userFromDb.getUsername(), userFromDb.getPassword());
    }

    public void createUser(UserRegistrationDto userRegistrationDto) {
        if (userRepository.findByUsername(userRegistrationDto.getEmail()) != null) {
            throw new RuntimeException("There is an account with that email address: " + userRegistrationDto.getEmail());
        }

        UserDto newUser = new UserDto();
        newUser.setFirstName(userRegistrationDto.getFirstName());
        newUser.setLastName(userRegistrationDto.getLastName());
        newUser.setUsername(userRegistrationDto.getEmail());
        newUser.setPassword(passwordEncoder.encode(userRegistrationDto.getPassword()));

        userRepository.save(newUser);
    }
}
