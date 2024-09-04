package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.ServiceResponse;
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

    public ServiceResponse<Void> subscribe(String email) {
        try {
            Optional<Subscriber> existingSubscriber = subscriberRepository.findByEmailIgnoreCase(email);

            if (existingSubscriber.isPresent()) {
                logger.info("Subscriber with email {} already exists.", email);
                return ServiceResponse.value(null); // No action needed, return success with no value
            }

            Subscriber subscriber = new Subscriber();
            subscriber.setEmail(email);
            subscriberRepository.save(subscriber);

            logger.info("Successfully subscribed email: {}", email);
            return ServiceResponse.value(null); // Successfully subscribed

        } catch (DataAccessException e) {
            return logAndReturnError("Database error occurred while subscribing email: " + email, "database_error", e);

        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while subscribing email: " + email, "unexpected_error", e);
        }
    }

    private <T> ServiceResponse<T> logAndReturnError(String message, String errorCode, Exception e) {
        logger.error(message, e);
        return ServiceResponse.error(errorCode);
    }

    private <T> ServiceResponse<T> logAndReturnError(String message, String errorCode) {
        logger.error(message);
        return ServiceResponse.error(errorCode);
    }
}
