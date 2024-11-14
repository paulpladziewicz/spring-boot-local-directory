package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.SearchHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SearchHistoryRepository extends MongoRepository<SearchHistory, String> {}

