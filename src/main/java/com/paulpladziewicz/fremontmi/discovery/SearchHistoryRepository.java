package com.paulpladziewicz.fremontmi.discovery;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SearchHistoryRepository extends MongoRepository<SearchHistory, String> {}

