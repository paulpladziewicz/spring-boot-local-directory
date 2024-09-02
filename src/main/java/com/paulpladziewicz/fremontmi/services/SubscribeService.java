package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Subscriber;
import com.paulpladziewicz.fremontmi.repositories.SubscriberRepository;
import org.springframework.stereotype.Service;

@Service
public class SubscribeService {

    private final SubscriberRepository subscriberRepository;

    public SubscribeService(SubscriberRepository subscriberRepository) {
        this.subscriberRepository = subscriberRepository;
    }

    public void subscribe(String email) {
        Subscriber existingSubscriber = subscriberRepository.findByEmailIgnoreCase(email);

        if (existingSubscriber == null) {
            Subscriber subscriber = new Subscriber();
            subscriber.setEmail(email);
            subscriberRepository.save(subscriber);
        }
    }
}
