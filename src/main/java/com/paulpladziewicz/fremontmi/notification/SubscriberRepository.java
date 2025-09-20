package com.paulpladziewicz.fremontmi.notification;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriberRepository extends MongoRepository<Subscriber, String> {
    Optional<Subscriber> findByEmailIgnoreCase(String email);
}

