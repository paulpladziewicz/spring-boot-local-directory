package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.SubscriberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final SubscriberRepository subscriberRepository;
    private final EmailService emailService;
    private final ContentService contentService;

    public NotificationService(SubscriberRepository subscriberRepository, EmailService emailService, ContentService contentService) {
        this.subscriberRepository = subscriberRepository;
        this.emailService = emailService;
        this.contentService = contentService;
    }

    public void subscribe(String email) {
        Optional<Subscriber> existingSubscriber = subscriberRepository.findByEmailIgnoreCase(email);

        if (existingSubscriber.isPresent()) {
            logger.info("Subscriber with email {} already exists.", email);
            ServiceResponse.value(null);
            return; // No action needed, return success with no value
        }

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriberRepository.save(subscriber);

        ServiceResponse.value(null);

    }

    public void unsubscribe(String email) {}

    public void handleContactFormSubmission(SimpleContactFormSubmission submission) {
        String recipient = "ppladziewicz@gmail.com";
        if (submission.getContentId() != null) {
            Content content = contentService.findById(submission.getContentId());
            String contentEmail = content.getDetail().getEmail();
            if (contentEmail != null) {
                recipient = contentEmail;
            }
        }

        emailService.simpleContactFormSubmission(recipient, submission);
    }


    public boolean emailParticipants(EmailRequest email) {
        return true;
    }

    public boolean emailAdministrators(EmailRequest email) {
        return true;
    }

    public Announcement createAnnouncement(String contentId, Announcement announcement) {
        return new Announcement();
    }

    public void deleteAnnouncement(String contentId, int announcementId) {

    }
}
