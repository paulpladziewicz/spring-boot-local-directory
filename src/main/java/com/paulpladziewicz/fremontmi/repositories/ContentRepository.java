package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.Content;
import com.paulpladziewicz.fremontmi.models.Event;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContentRepository extends MongoRepository<Content, String> {
    @Query("{ 'id': ?0 }")
    <T extends Content> Optional<T> findById(String id, Class<T> clazz);

    @Query("{ '_id': { $in: ?0 } }")
    <T extends Content> List<T> findAllById(List<String> ids, Class<T> clazz);

    @Query("{ 'slug': ?0, 'type': ?1 }")
    Optional<Content> findBySlugAndType(String slug, String contentType);

    @Query("{ 'slug': ?0, 'type': ?1 }")
    <T extends Content> Optional<T> findBySlugAndType(String slug, String contentType, Class<T> clazz);

    @Query("{ 'slug': { $regex: ?0 }, 'type': ?1 }")
    List<Content> findBySlugRegexAndType(String slugPattern, String contentType);

    // remove after migration
    @Query("{ 'type': ?0, 'visibility': 'public' }")
    List<Content> findAllByType(String contentType);

    // remove after migration
    @Query("{ 'tags': ?0, 'type':  ?1, 'visibility': ContentVISIBILITY.PUBLIC.getVisibility() }")
    List<Content> findByTagAndType(String tag, String contentType);

    @Query("{ 'type': ?0, 'visibility': 'public' }")
    <T extends Content> List<T> findAllByType(String contentType, Class<T> clazz);

    @Query("{ 'tags': ?0, 'type':  ?1, 'visibility': ContentVISIBILITY.PUBLIC.getVisibility() }")
    <T extends Content> List<T> findByTagAndType(String tag, String contentType, Class<T> clazz);

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

    @Query("{'visibility': 'public', 'days': { $elemMatch: { 'endTime': { $gt: ?0 } } }, 'status': { $in: ['active', 'canceled'] }}")
    List<Event> findByAnyFutureDayEventOrderBySoonestStartTimeAsc(LocalDateTime now, Sort sort);

    @Query("{ 'visibility': 'public', 'tags': { $in: [?0] }, 'days': { $elemMatch: { 'endTime': { $gt: ?1 } } }, 'status': { $in: ['active', 'canceled'] }}")
    List<Event> findByTagAndAnyFutureDayEventOrderBySoonestStartTimeAsc(String tag, LocalDateTime now, Sort sort);

}
