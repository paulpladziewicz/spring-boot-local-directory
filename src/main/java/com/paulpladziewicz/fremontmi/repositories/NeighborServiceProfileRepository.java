package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.NeighborService;
import com.paulpladziewicz.fremontmi.models.NeighborServiceProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NeighborServiceProfileRepository extends MongoRepository<NeighborServiceProfile, String> {
    List<NeighborServiceProfile> findByStatus(String active);
}
