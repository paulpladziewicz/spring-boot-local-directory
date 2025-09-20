package com.paulpladziewicz.fremontmi.content;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentVectorRepository extends MongoRepository<ContentVector, String> {}