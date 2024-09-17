package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.Content;
import com.paulpladziewicz.fremontmi.models.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ContentRepository extends MongoRepository<Content, String> {
    Optional<Content> findBySlug(String slug);

    List<Content> findBySlugRegex(String slugPattern);

    List<Content> findAllByType(String contentType);

//    @Query("{ '_id': ?0, 'type': 'group' }")
//    Optional<Group> findGroupById(String id);
//
//    @Query("{ 'slug': ?0, 'type': 'group' }")
//    Optional<Group> findGroupBySlug(String slug);

    @Query("{ 'reviewed': false }")
    List<Content> contentNotReviewed();
}
