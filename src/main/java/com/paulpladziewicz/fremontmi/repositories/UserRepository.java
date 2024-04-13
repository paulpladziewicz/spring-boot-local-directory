package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.UserDto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface UserRepository extends MongoRepository<UserDto, String> {

    @Query("{username:'?0'}")
    UserDto findByUsername(String username);

    UserDto findByResetPasswordToken(String token);
}
