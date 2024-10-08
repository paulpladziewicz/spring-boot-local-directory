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
    <T extends Content> Optional<T> findBySlugAndType(String slug, String contentType, Class<T> clazz);

    @Query("{ 'slug': { $regex: ?0 }, 'type': ?1 }")
    List<Content> findBySlugRegexAndType(String slugPattern, String contentType);

    @Query("{ 'type': ?0, 'visibility': 'public' }")
    <T extends Content> List<T> findAllByType(String contentType, Class<T> clazz);

    @Query("{ 'tags': ?0, 'type':  ?1, 'visibility': 'public' }")
    <T extends Content> List<T> findByTagAndType(String tag, String contentType, Class<T> clazz);

    @Query("{ 'type': 'neighbor-services-profile', 'createdBy': ?0 }")
    <T extends Content> Optional<T> findByCreatedBy(String createdBy, Class<T> clazz);

    @Query("{ 'stripeDetails.subscriptionId': ?0 }")
    <T extends Content> Optional<T> findByStripeDetails_SubscriptionId(String subscriptionId, Class<T> clazz);

    @Query("{ 'stripeDetails.invoiceId': ?0 }")
    <T extends Content> Optional<T> findByStripeDetails_InvoiceId(String invoiceId, Class<T> clazz);

    @Query("{'visibility': 'public', 'days': { $elemMatch: { 'endTime': { $gt: ?0 } } }, 'status': { $in: ['active', 'canceled'] }}")
    List<Event> findByAnyFutureDayEventOrderBySoonestStartTimeAsc(LocalDateTime now, Sort sort);

    @Query("{ 'visibility': 'public', 'tags': { $in: [?0] }, 'days': { $elemMatch: { 'endTime': { $gt: ?1 } } }, 'status': { $in: ['active', 'canceled'] }}")
    List<Event> findByTagAndAnyFutureDayEventOrderBySoonestStartTimeAsc(String tag, LocalDateTime now, Sort sort);
}

