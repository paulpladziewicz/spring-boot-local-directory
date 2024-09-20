package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.Content;
import com.paulpladziewicz.fremontmi.models.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends MongoRepository<Tag, String> {

    @Query("{ 'reviewed': false }")
    List<Content> tagsNotReviewed();

    Optional<Tag> findByName(String canonicalName);
}
