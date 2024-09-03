package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.ServiceResult;
import com.paulpladziewicz.fremontmi.models.Subscriber;
import com.paulpladziewicz.fremontmi.repositories.SubscriberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SubscribeService {

    private static final Logger logger = LoggerFactory.getLogger(SubscribeService.class);

    private final SubscriberRepository subscriberRepository;

    public SubscribeService(SubscriberRepository subscriberRepository) {
        this.subscriberRepository = subscriberRepository;
    }

    public ServiceResult<Void> subscribe(String email) {
        try {
            Optional<Subscriber> existingSubscriber = subscriberRepository.findByEmailIgnoreCase(email);

            if (existingSubscriber.isPresent()) {
                logger.info("Subscriber with email {} already exists.", email);
                return ServiceResult.success();
            }

            Subscriber subscriber = new Subscriber();
            subscriber.setEmail(email);
            subscriberRepository.save(subscriber);

            logger.info("Successfully subscribed email: {}", email);
            return ServiceResult.success();

        } catch (DataAccessException e) {
            logger.error("Database error occurred while subscribing email: {}", email, e);
            return ServiceResult.error("Failed to subscribe due to a database error. Please try again later.", "database_error");

        } catch (Exception e) {
            logger.error("Unexpected error occurred while subscribing email: {}", email, e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }
}