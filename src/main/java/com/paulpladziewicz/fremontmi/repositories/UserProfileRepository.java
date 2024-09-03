package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
    Optional<UserProfile> findByEmail(String email);

    boolean existsByEmail(String email);
}
