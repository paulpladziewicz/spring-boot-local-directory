package com.paulpladziewicz.fremontmi.content;

import org.springframework.stereotype.Service;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.user.UserProfile;
import com.paulpladziewicz.fremontmi.user.UserService;

import java.util.*;

@Service
public class InteractionService {

    private final ContentService contentService;
    private final UserService userService;

    public InteractionService(ContentService contentService, UserService userService) {
        this.contentService = contentService;
        this.userService = userService;
    }

    public void heart(String contentId) {
        UserProfile userProfile = userService.getUserProfile();
        Content content = contentService.findById(contentId);
        Set<String> heartedUserIds = content.getHeartedUserIds();

        boolean isAdded = heartedUserIds.add(userProfile.getUserId());

        if (isAdded) {
            content.setHeartCount(content.getHeartCount() + 1);
            userProfile.getContentActions()
                    .computeIfAbsent(content.getType(), k -> new HashMap<>())
                    .computeIfAbsent(ContentAction.HEARTED, k -> new HashSet<>())
                    .add(contentId);
            userService.saveUserProfile(userProfile);
            contentService.save(content);
        }
    }

    public void bookmark(String contentId) {
        UserProfile userProfile = userService.getUserProfile();
        Content content = contentService.findById(contentId);

        boolean isAdded = userProfile.getContentActions()
                .computeIfAbsent(content.getType(), k -> new HashMap<>())
                .computeIfAbsent(ContentAction.BOOKMARKED, k -> new HashSet<>())
                .add(contentId);

        if (isAdded) {
            userService.saveUserProfile(userProfile);
        }
    }

    public void addParticipant(String contentId) {
        UserProfile userProfile = userService.getUserProfile();
        Content content = contentService.findById(contentId);

        boolean participantAdded = content.getParticipants().add(userProfile.getUserId());

        if (participantAdded) {
            userProfile.getContentActions()
                    .computeIfAbsent(content.getType(), k -> new HashMap<>())
                    .computeIfAbsent(ContentAction.PARTICIPATING, k -> new HashSet<>())
                    .add(contentId);
            userService.saveUserProfile(userProfile);
            contentService.save(content);
        }
    }

    public void removeParticipant(String contentId) {
        UserProfile userProfile = userService.getUserProfile();
        Content content = contentService.findById(contentId);

        boolean participantRemoved = content.getParticipants().remove(userProfile.getUserId());

        if (participantRemoved) {
            Map<ContentType, Map<ContentAction, Set<String>>> contentActions = userProfile.getContentActions();
            Set<String> participatingContentIds = contentActions.getOrDefault(content.getType(), new HashMap<>())
                    .getOrDefault(ContentAction.PARTICIPATING, new HashSet<>());
            participatingContentIds.remove(contentId);
            if (participatingContentIds.isEmpty()) {
                contentActions.get(content.getType()).remove(ContentAction.PARTICIPATING);
            }
            userService.saveUserProfile(userProfile);
            contentService.save(content);
        }
    }

    public void cancel(String contentId) {
        Content content = contentService.findById(contentId);
        content.setStatus(ContentStatus.CANCELLED);
        contentService.save(content);
    }

    public void reactivate(String contentId) {
        Content content = contentService.findById(contentId);
        content.setStatus(ContentStatus.ACTIVE);
        contentService.save(content);
    }
}
