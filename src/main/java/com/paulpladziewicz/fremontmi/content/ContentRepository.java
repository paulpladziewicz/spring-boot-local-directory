package com.paulpladziewicz.fremontmi.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContentRepository extends MongoRepository<Content, String> {

    List<Content> findByIdIn(List<String> contentIds);

    @Query("{ 'pathname': ?0, 'type': ?1 }")
    Optional<Content> findByPathname(String pathname, ContentType type);

    @Query("{ 'pathname': { $regex: ?0 }, 'type': ?1 }")
    List<Content> findByPathnameRegexAndType(String slugPattern, ContentType contentType);

    @Query("{ 'type': ?0, 'visibility': ?1 }")
    Page<Content> findByTypeAndVisibility(ContentType contentType, ContentVisibility visibility, Pageable pageable);

    @Query("{ 'type':  ?0, 'visibility': ?1, 'tags': ?2, }")
    Page<Content> findByTypeVisibilityAndTag(ContentType contentType, ContentVisibility visibility, String tag, Pageable pageable);

    // TODO find by profile value here...
    @Query("{ 'type': ?0, 'createdBy': ?1 }")
    Optional<Content> findByTypeAndUserCreatedBy(ContentType contentType, String userId);

    @Query("{ 'detail.days.startTime': { $gte: ?0 } }")
    Page<Content> findEventsAfterStartTime(LocalDateTime startTime, Pageable pageable);

    @Query(value = "{ 'visibility': 'PUBLIC' }", fields = "{ 'pathname': 1 }")
    List<Content> findAllPublicContentPathnames();
}

