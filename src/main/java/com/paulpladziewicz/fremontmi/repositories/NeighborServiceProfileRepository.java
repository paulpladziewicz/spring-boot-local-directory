package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.NeighborService;
import com.paulpladziewicz.fremontmi.models.NeighborServiceProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NeighborServiceProfileRepository extends MongoRepository<NeighborServiceProfile, String> {
    // Fetch all active profiles (status is always "active")
    List<NeighborServiceProfile> findByStatus(String status);

    // Fetch profiles by tag where status is "active"
    List<NeighborServiceProfile> findByTagsContainingAndStatus(String tag, String status);

    // Fetch profiles where status is always "active"
    @Query("{ 'status': 'active' }")
    List<NeighborServiceProfile> findByStatusActive();

    // Fetch profiles by tag where status is always "active"
    @Query("{ 'tags': ?0, 'status': 'active' }")
    List<NeighborServiceProfile> findByTagsContainingAndStatusActive(String tag);

    @Query(value = "{}", fields = "{ 'tags': 1 }")
    List<String> findDistinctTags();

    Optional<NeighborServiceProfile> findByUserId(String userId);
}
