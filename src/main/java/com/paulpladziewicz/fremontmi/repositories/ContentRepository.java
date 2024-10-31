package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.Content;
import com.paulpladziewicz.fremontmi.models.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ContentRepository extends MongoRepository<Content, String> {

    List<Content> findByIdIn(List<String> contentIds);

    @Query("{ 'pathname': ?0, 'type': ?1 }")
    Optional<Content> findByPathname(String pathname, ContentType type);

    @Query("{ 'pathname': { $regex: ?0 }, 'type': ?1 }")
    List<Content> findByPathnameRegexAndType(String slugPattern, ContentType contentType);

    @Query("{ 'type': ?0, 'visibility': 'public' }")
    Page<Content> findByTypeAndVisibility(ContentType contentType, Pageable pageable);

    @Query("{ 'tags': ?0, 'type':  ?1, 'visibility': 'public' }")
    Page<Content> findPublicContentByTagAndType(String tag, ContentType contentType, Pageable pageable);

    // TODO find by profile value here...
    @Query("{ 'type': ?0, 'createdBy': ?1 }")
    Optional<Content> findByTypeAndUserCreatedBy(ContentType contentType, String userId);
}

