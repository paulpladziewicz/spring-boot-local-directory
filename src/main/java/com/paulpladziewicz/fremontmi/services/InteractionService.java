package com.paulpladziewicz.fremontmi.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.paulpladziewicz.fremontmi.models.*;

import java.util.List;
import java.util.Set;


@Service
public class InteractionService {

    private static final Logger logger = LoggerFactory.getLogger(InteractionService.class);
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
            UserProfile.ContentActions actions = userProfile.getContentActionsByType().computeIfAbsent(content.getType(), k -> new UserProfile.ContentActions());
            actions.getHearted().add(content.getId());
        } else {
            return;
        }

        userService.saveUserProfile(userProfile);
        contentService.save(content);
    }

    public void bookmark(String contentId) {
        UserProfile userProfile = userService.getUserProfile();
        Content content = contentService.findById(contentId);
        UserProfile.ContentActions actions = userProfile.getContentActionsByType().computeIfAbsent(content.getType(), k -> new UserProfile.ContentActions());

        boolean isAdded = actions.getBookmarked().add(content.getId());

        if (isAdded) {
            userService.saveUserProfile(userProfile);
        } else {
            return;
        }
    }

    public void join(String contentId) {
        UserProfile userProfile = userService.getUserProfile();
        Content content = contentService.findById(contentId);

        if (content.getDetail() instanceof Group group) {
            List<String> members = group.getMembers();
            if (!members.contains(userProfile.getUserId())) {
                members.add(userProfile.getUserId());
                group.setMembers(members);
            } else {
                return;
            }
        }

        contentService.save(content);
    }

    public void leave(String contentId) {
        UserProfile userProfile = userService.getUserProfile();
        Content content = contentService.findById(contentId);

        if (content.getDetail() instanceof Group group) {
            List<String> members = group.getMembers();
            if (members.contains(userProfile.getUserId())) {
                members.remove(userProfile.getUserId());
                group.setMembers(members);
            } else {
                return;
            }
        }

        contentService.save(content);
    }

    public void cancel(String contentId) {

    }

    public void reverseCancel(String contentId) {

    }
}
