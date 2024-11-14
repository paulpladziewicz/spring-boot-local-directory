package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.ContentVector;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentVectorRepository extends MongoRepository<ContentVector, String> {}