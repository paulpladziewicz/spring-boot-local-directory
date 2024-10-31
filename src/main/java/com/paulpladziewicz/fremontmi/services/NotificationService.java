package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.exceptions.ContentNotFoundException;
import com.paulpladziewicz.fremontmi.exceptions.ValidationException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.SubscriberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final SubscriberRepository subscriberRepository;
    private final EmailService emailService;
    private final ContentService contentService;
    private final UserService userService;

    public NotificationService(SubscriberRepository subscriberRepository, EmailService emailService, ContentService contentService, UserService userService) {
        this.subscriberRepository = subscriberRepository;
        this.emailService = emailService;
        this.contentService = contentService;
        this.userService = userService;
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
            try {
                Content content = contentService.findById(submission.getContentId());
                String contentEmail = content.getDetail().getEmail();
                if (contentEmail != null) {
                    recipient = contentEmail;
                }
            } catch (ContentNotFoundException ignored) {}
        }

        emailService.simpleContactFormSubmission(recipient, submission);
    }

    public boolean email(EmailRequest emailRequest) {
        UserProfile senderUserProfile = userService.getUserProfile();

        if (senderUserProfile.getEmailSendCount() >= 5) {
            throw new ValidationException("User has reached the email limit.");
        }

        senderUserProfile.setEmailSendCount(senderUserProfile.getEmailSendCount() + 1);

        userService.saveUserProfile(senderUserProfile);

        Content content = contentService.findById(emailRequest.getContentId());
        String userId = senderUserProfile.getUserId();

        boolean response;
        if (content.getAdministrators().contains(userId)) {
            response = emailParticipants(content, senderUserProfile, emailRequest);

        } else {
            response = emailAdministrators(content, senderUserProfile, emailRequest);
        }

        return response;
    }

    public boolean emailParticipants(Content content, UserProfile userProfile, EmailRequest emailRequest) {
        List<UserProfile> memberUserProfiles = userService.getUserProfiles(content.getParticipants());

        List<String> emailAddresses = memberUserProfiles.stream()
                .map(UserProfile::getEmail)
                .filter(email -> email != null && !email.isEmpty())
                .collect(Collectors.toList());

        return emailService.sendGroupEmail(emailAddresses, userProfile.getEmail(), userProfile.getFirstName() + ' ' + userProfile.getLastName(), content.getDetail().getTitle(), emailRequest.getSubject(), emailRequest.getMessage());
    }

    public boolean emailAdministrators(Content content, UserProfile userProfile, EmailRequest emailRequest) {
        List<UserProfile> memberUserProfiles = userService.getUserProfiles(content.getAdministrators());

        List<String> emailAddresses = memberUserProfiles.stream()
                .map(UserProfile::getEmail)
                .filter(email -> email != null && !email.isEmpty())
                .collect(Collectors.toList());

        return emailService.sendGroupEmail(emailAddresses, userProfile.getEmail(), userProfile.getFirstName() + ' ' + userProfile.getLastName(), content.getDetail().getTitle(), emailRequest.getSubject(), emailRequest.getMessage());

    }

    public void createAnnouncement(AnnouncementDto announcementDto) {
        Content content = contentService.findById(announcementDto.getContentId());
        contentService.checkPermission(content);

        if (content.getDetail() instanceof Group group) {
            Announcement announcement = new Announcement();

            int id = group.getAnnouncements().isEmpty() ? 1 : group.getAnnouncements().getFirst().getId() + 1;
            announcement.setId(id);

            announcement.setTitle(announcementDto.getTitle());
            announcement.setMessage(announcementDto.getMessage());
            announcement.setCreatedAt(Instant.now());

            group.getAnnouncements().addFirst(announcement);

            contentService.save(content);
        } else {
            throw new IllegalArgumentException("Invalid content");
        }
    }

    public void deleteAnnouncement(AnnouncementDto announcementDto) {
        Content content = contentService.findById(announcementDto.getContentId());
        contentService.checkPermission(content);

        if (content.getDetail() instanceof Group group) {
            List<Announcement> announcements = group.getAnnouncements();
            announcements.removeIf(announcement -> announcement.getId() == announcementDto.getId());
            contentService.save(content);
        } else {
            throw new IllegalArgumentException("Invalid content");
        }
    }
}
