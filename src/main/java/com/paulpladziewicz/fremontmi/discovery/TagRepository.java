package com.paulpladziewicz.fremontmi.discovery;

import com.paulpladziewicz.fremontmi.content.Content;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends MongoRepository<Tag, String> {

    @Query("{ 'reviewed': false }")
    List<Content> tagsNotReviewed();

    Optional<Tag> findByName(String canonicalName);
}
