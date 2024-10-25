package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.models.Subscriber;
import com.paulpladziewicz.fremontmi.repositories.SubscriberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final SubscriberRepository subscriberRepository;

    public NotificationService(SubscriberRepository subscriberRepository) {
        this.subscriberRepository = subscriberRepository;
    }

    public ServiceResponse<Void> subscribe(String email) {
        Optional<Subscriber> existingSubscriber = subscriberRepository.findByEmailIgnoreCase(email);

        if (existingSubscriber.isPresent()) {
            logger.info("Subscriber with email {} already exists.", email);
            return ServiceResponse.value(null); // No action needed, return success with no value
        }

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriberRepository.save(subscriber);

        return ServiceResponse.value(null);

    }
}
