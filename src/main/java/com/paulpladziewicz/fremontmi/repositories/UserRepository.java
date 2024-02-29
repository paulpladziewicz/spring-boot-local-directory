package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    User findByUsername(String username);
}
