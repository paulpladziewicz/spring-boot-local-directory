package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.Content;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ContentRepository extends MongoRepository<Content, String> {
    Optional<Content> findBySlug(String slug);

    List<Content> findBySlugRegex(String slugPattern);
}
