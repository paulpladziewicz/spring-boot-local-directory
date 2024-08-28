package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.UserDetailsDto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDetailsRepository extends MongoRepository<UserDetailsDto, String> {
    UserDetailsDto findByEmail(String email);
}
