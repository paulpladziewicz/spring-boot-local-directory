package com.paulpladziewicz.fremontmi.user;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserRecord, String> {

    @Query("{username:'?0'}")
    Optional<UserRecord> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<UserRecord> findByResetPasswordToken(String token);

    @Query("{confirmationToken:'?0'}")
    UserRecord findByConfirmationToken(String token);
}
