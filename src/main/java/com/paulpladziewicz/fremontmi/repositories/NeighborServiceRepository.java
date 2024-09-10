package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.NeighborService;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NeighborServiceRepository extends MongoRepository<NeighborService, String> {
    List<NeighborService> findByStatus(String status);
}