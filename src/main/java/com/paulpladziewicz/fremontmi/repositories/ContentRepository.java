package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.Content;
import com.paulpladziewicz.fremontmi.models.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContentRepository extends MongoRepository<Content, String> {
    @Query("{ 'slug': ?0, 'type': ?1 }")
    Optional<Content> findBySlugAndType(String slug, String contentType);

    @Query("{ 'slug': { $regex: ?0 }, 'type': ?1 }")
    List<Content> findBySlugRegexAndType(String slugPattern, String contentType);

    @Query("{ 'type': ?0, 'visibility': 'public' }")
    List<Content> findAllByType(String contentType);

    @Query("{ 'tags': ?0, 'type':  ?1, 'visibility': ContentVISIBILITY.PUBLIC.getVisibility() }")
    List<Content> findByTagAndType(String tag, String contentType);

    @Query("{ 'type': 'neighbor-services-profile', 'createdBy': ?0 }")
    Optional<Content> findByCreatedBy(String createdBy);

    @Query(value = "{}", fields = "{ 'tags': 1 }")
    List<String> findDistinctTags();

    @Query("{ 'reviewed': false }")
    List<Content> contentNotReviewed();

    @Query("{'visibility': 'public', 'days': { $elemMatch: { 'endTime': { $gt: ?0 } } }, 'status': { $in: ['active', 'canceled'] } }")
    List<Event> findByAnyFutureDayEvent(LocalDateTime now);

    @Query("{ 'visibility': 'public', 'tags': { $in: [?0] }, 'days': { $elemMatch: { 'endTime': { $gt: ?1 } } }, 'status': { $in: ['active', 'canceled'] } }")
    List<Event> findByTagAndAnyFutureDayEvent(String tag, LocalDateTime now);

    @Query("{ 'stripeDetails.subscriptionId': ?0 }")
    Optional<Content> findByStripeDetails_SubscriptionId(String subscriptionId);

    @Query("{ 'stripeDetails.invoiceId': ?0 }")
    Optional<Content> findByStripeDetails_InvoiceId(String invoiceId);
}
