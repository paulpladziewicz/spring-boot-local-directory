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

    List<Content> findByIdIn(List<String> contentIds);

    @Query("{ 'pathname': ?0, 'type': ?1 }")
    <T extends Content> Optional<T> findByPathname(String slug);

    @Query("{ 'pathname': { $regex: ?0 }, 'type': ?1 }")
    List<Content> findByPathnameRegexAndType(String slugPattern, String contentType);

    @Query("{ 'type': ?0, 'visibility': 'public' }")
    List<Content> findByType(String contentType);

    @Query("{ 'tags': ?0, 'type':  ?1, 'visibility': 'public' }")
    List<Content> findByTagAndType(String tag, String contentType);

    @Query("{ 'type': 'neighbor-services-profile', 'createdBy': ?0 }")
    <T extends Content> Optional<T> findByCreatedBy(String createdBy, Class<T> clazz);

    @Query("{ 'stripeDetails.subscriptionId': ?0 }")
    <T extends Content> Optional<T> findByStripeDetails_SubscriptionId(String subscriptionId);

    @Query("{ 'stripeDetails.invoiceId': ?0 }")
    <T extends Content> Optional<T> findByStripeDetails_InvoiceId(String invoiceId);

    @Query("{'visibility': 'public', 'days': { $elemMatch: { 'endTime': { $gt: ?0 } } }, 'status': { $in: ['active', 'canceled'] }}")
    List<Event> findByAnyFutureDayEventOrderBySoonestStartTimeAsc(LocalDateTime now, Sort sort);

    @Query("{ 'visibility': 'public', 'tags': { $in: [?0] }, 'days': { $elemMatch: { 'endTime': { $gt: ?1 } } }, 'status': { $in: ['active', 'canceled'] }}")
    List<Event> findByTagAndAnyFutureDayEventOrderBySoonestStartTimeAsc(String tag, LocalDateTime now, Sort sort);
}

