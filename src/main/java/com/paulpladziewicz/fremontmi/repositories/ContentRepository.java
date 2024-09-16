package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.Content;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ContentRepository extends MongoRepository<Content, String> {
}
