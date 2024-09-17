package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

    private final ContentRepository contentRepository;

    private final UserService userService;

    public GroupService(ContentRepository contentRepository, UserService userService) {
        this.contentRepository = contentRepository;
        this.userService = userService;
    }

    @Transactional
    public ServiceResponse<Group> createGroup(Group group) {
        try {
            Optional<UserProfile> userProfileOpt = userService.getUserProfile();

            if (userProfileOpt.isEmpty()) {
                return logAndReturnError("Failed to create group: user profile not found.", "user_profile_not_found");
            }

            UserProfile userProfile = userProfileOpt.get();

            group.setMembers(List.of(userProfile.getUserId()));
            group.setAdministrators(List.of(userProfile.getUserId()));
            group.setType(ContentTypes.GROUP.getContentType());
            group.setSlug(createUniqueSlug(group.getName()));
            group.setCreatedBy(userProfile.getUserId());

            Group savedGroup = contentRepository.save(group);

            userService.addContentIdToUserProfile(ContentTypes.GROUP, savedGroup.getId());

            return ServiceResponse.value(savedGroup);

        } catch (DataAccessException e) {
            return logAndReturnError("Failed to save group due to a database error", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while creating group.", "unexpected_error", e);
        }
    }

    // TODO make this more efficient
    public String createUniqueSlug(String name) {
        // Clean up the name to form the base slug
        String baseSlug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");

        // Find all slugs that start with the base slug
        List<Content> matchingSlugs = contentRepository.findBySlugRegex("^" + baseSlug + "(-\\d+)?$");

        // If no matching slugs, return the base slug
        if (matchingSlugs.isEmpty()) {
            return baseSlug;
        }

        // Extract slugs that match the baseSlug-<number> format
        Pattern pattern = Pattern.compile(Pattern.quote(baseSlug) + "-(\\d+)$");

        int maxNumber = 0;
        boolean baseSlugExists = false;

        for (Content content : matchingSlugs) {
            String slug = content.getSlug();

            // Check if the base slug without a number already exists
            if (slug.equals(baseSlug)) {
                baseSlugExists = true;
            }

            // Find the slugs with numbers at the end and get the max number
            Matcher matcher = pattern.matcher(slug);
            if (matcher.find()) {
                int number = Integer.parseInt(matcher.group(1));
                maxNumber = Math.max(maxNumber, number);
            }
        }

        // If the base slug already exists, start numbering from 1
        if (baseSlugExists) {
            return baseSlug + "-" + (maxNumber + 1);
        } else if (maxNumber > 0) {
            return baseSlug + "-" + (maxNumber + 1);
        } else {
            return baseSlug;  // No suffix needed if base slug doesn't exist
        }
    }

    public ServiceResponse<List<Group>> findAll() {
        try {
            List<Content> contentList = contentRepository.findAllByType(ContentTypes.GROUP.getContentType());

            List<Group> groupList = contentList.stream()
                    .filter(content -> content instanceof Group)
                    .map(content -> (Group) content)
                    .collect(Collectors.toList());

            return ServiceResponse.value(groupList);
        } catch (DataAccessException e) {
            return logAndReturnError("Failed to retrieve groups due to a database error", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while retrieving groups.", "unexpected_error", e);
        }
    }

    public ServiceResponse<Group> findBySlug(String slug) {
        try {
            Optional<Content> groupOpt = contentRepository.findBySlug(slug);
            return groupOpt
                    .map(content -> (Group) content)
                    .map(ServiceResponse::value)
                    .orElseGet(() -> logAndReturnError("Group not found with slug: " + slug, "group_not_found"));
        } catch (DataAccessException e) {
            return logAndReturnError("Failed to retrieve group with slug " + slug + " due to a database error", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while retrieving group with slug " + slug, "unexpected_error", e);
        }
    }

    public ServiceResponse<Group> findGroupById(String id) {
        try {
            Optional<Content> optionalGroupContent = contentRepository.findById(id);
            return optionalGroupContent
                    .map(content -> (Group) content)
                    .map(ServiceResponse::value)
                    .orElseGet(() -> logAndReturnError("Group not found with content id: " + id, "group_not_found"));
        } catch (DataAccessException e) {
            return logAndReturnError("Failed to retrieve group with content id " + id + " due to a database error", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while retrieving group with content id " + id, "unexpected_error", e);
        }
    }

    public ServiceResponse<List<Group>> findGroupsByUser() {
        try {
            Optional<UserProfile> userProfileOpt = userService.getUserProfile();
            if (userProfileOpt.isEmpty()) {
                return logAndReturnError("Failed to retrieve groups: user profile not found.", "user_profile_not_found");
            }

            UserProfile userProfile = userProfileOpt.get();
            List<Content> contentList = contentRepository.findAllById(userProfile.getGroupIds());

            List<Group> groups = contentList.stream()
                    .filter(content -> content instanceof Group)
                    .map(content -> (Group) content)
                    .collect(Collectors.toList());

            return ServiceResponse.value(groups);

        } catch (DataAccessException e) {
            return logAndReturnError("Failed to retrieve groups for user due to a database error", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while retrieving groups for user.", "unexpected_error", e);
        }
    }

    @Transactional
    public ServiceResponse<Content> updateGroup(String contentId, Group group) {
        try {
            Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

            if (optionalUserProfile.isEmpty()) {
                return logAndReturnError("User profile not found", "user_profile_not_found");
            }

            UserProfile userProfile = optionalUserProfile.get();

            ServiceResponse<Group> findGroupResponse = findGroupById(contentId);

            if (findGroupResponse.hasError()) {
                return ServiceResponse.error(findGroupResponse.errorCode());
            }

            Group existingGroup = findGroupResponse.value();

            if (!hasPermission(userProfile, existingGroup)) {
                return logAndReturnError("User doesn't have permission to delete group", "permission_denied");
            }

            // TODO: make it's own function for updating group properties
            existingGroup.setName(group.getName());
            existingGroup.setDescription(group.getDescription());
            existingGroup.setTags(group.getTags());


            return ServiceResponse.value(contentRepository.save(existingGroup));

        } catch (DataAccessException e) {
            return logAndReturnError("Failed to update group due to a database error", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while updating group.", "unexpected_error", e);
        }
    }

    @Transactional
    public ServiceResponse<Void> deleteGroup(String contentId) {
        try {
            Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

            if (optionalUserProfile.isEmpty()) {
                return logAndReturnError("User profile not found", "user_profile_not_found");
            }

            UserProfile userProfile = optionalUserProfile.get();

            ServiceResponse<Group> findGroupResponse = findGroupById(contentId);

            if (findGroupResponse.hasError()) {
                return ServiceResponse.error(findGroupResponse.errorCode());
            }

            Group groupContent = findGroupResponse.value();

            if (!hasPermission(userProfile, groupContent)) {
                return logAndReturnError("User doesn't have permission to delete group", "permission_denied");
            }

            // Remove group from user profile
            // Need to decide if I am really deleting the group or just removing it from the user profile & changing visibility and status
            contentRepository.deleteById(contentId);
            logger.info("Successfully deleted group with content id: {}", contentId);
            return ServiceResponse.value(null);

        } catch (DataAccessException e) {
            return logAndReturnError("Failed to delete group due to a database error", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while deleting group.", "unexpected_error", e);
        }
    }

    private Boolean hasPermission(UserProfile userProfile, Group group) {
        return group.getAdministrators().contains(userProfile.getUserId());
    }

    @Transactional
    public ServiceResponse<Boolean> joinGroup(String contentId) {
        Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

        if (optionalUserProfile.isEmpty()) {
            return logAndReturnError("User profile not found", "user_profile_not_found");
        }

        UserProfile userProfile = optionalUserProfile.get();

        ServiceResponse<Group> findGroupResponse = findGroupById(contentId);

        if (findGroupResponse.hasError()) {
            return ServiceResponse.error(findGroupResponse.errorCode());
        }

        Group group = findGroupResponse.value();

        List<String> members = group.getMembers();
        if (!members.contains(userProfile.getUserId())) {
            members.add(userProfile.getUserId());
            group.setMembers(members);
            contentRepository.save(group);
        }
        List<String> groupIds = userProfile.getGroupIds();
        if (!groupIds.contains(contentId)) {
            groupIds.add(contentId);
            userProfile.setGroupIds(groupIds);
            userService.saveUserProfile(userProfile);
        }

        return ServiceResponse.value(true);
    }

    @Transactional
    public ServiceResponse<Boolean> leaveGroup(String groupId) {
        Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

        if (optionalUserProfile.isEmpty()) {
            return logAndReturnError("User profile not found", "user_profile_not_found");
        }

        UserProfile userProfile = optionalUserProfile.get();

        ServiceResponse<Group> findGroupResponse = findGroupById(groupId);

        if (findGroupResponse.hasError()) {
            return ServiceResponse.error(findGroupResponse.errorCode());
        }

        Group group = findGroupResponse.value();

        List<String> members = new ArrayList<>(group.getMembers());
        members.remove(userProfile.getUserId());
        group.setMembers(members);
        Content savedGroupContent = contentRepository.save(group);

        List<String> userProfileGroupIds = new ArrayList<>(userProfile.getGroupIds());
        userProfileGroupIds.remove(savedGroupContent.getId());
        userProfile.setGroupIds(userProfileGroupIds);
        userService.saveUserProfile(userProfile);

        return ServiceResponse.value(true);
    }

    public ServiceResponse<List<Announcement>> addAnnouncement(String contentId, Announcement announcement) {
        Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

        if (optionalUserProfile.isEmpty()) {
            return logAndReturnError("User profile not found", "user_profile_not_found");
        }

        UserProfile userProfile = optionalUserProfile.get();

        ServiceResponse<Group> findGroupResponse = findGroupById(contentId);

        if (findGroupResponse.hasError()) {
            return ServiceResponse.error(findGroupResponse.errorCode());
        }

        Group group = findGroupResponse.value();

        if (!hasPermission(userProfile, group)) {
            return logAndReturnError("User doesn't have permission to delete group", "permission_denied");
        }

        announcement.setId(group.getAnnouncements().size() + 1);
        List<Announcement> announcements = new ArrayList<>(group.getAnnouncements());
        announcements.addFirst(announcement);
        group.setAnnouncements(announcements);

        try {
            contentRepository.save(group);
        } catch (DataIntegrityViolationException e) {
            return logAndReturnError("Failed to add an announcement due to a database error when saving the group.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while deleting group.", "unexpected_error", e);
        }

        return ServiceResponse.value(announcements);
    }

    public ServiceResponse<Boolean> deleteAnnouncement(String groupId, int announcementId) {
        Optional<UserProfile> optionalUserProfile = userService.getUserProfile();

        if (optionalUserProfile.isEmpty()) {
            return logAndReturnError("User profile not found", "user_profile_not_found");
        }

        UserProfile userProfile = optionalUserProfile.get();

        ServiceResponse<Group> findGroupResponse = findGroupById(groupId);

        if (findGroupResponse.hasError()) {
            return ServiceResponse.error(findGroupResponse.errorCode());
        }

        Group group = findGroupResponse.value();

        if (!hasPermission(userProfile, group)) {
            return logAndReturnError("User doesn't have permission to delete group", "permission_denied");
        }

        List<Announcement> announcements = new ArrayList<>(group.getAnnouncements());

        boolean isDeleted = false;

        for (int i = 0; i < announcements.size(); i++) {
            Announcement currentAnnouncement = announcements.get(i);
            if (currentAnnouncement.getId() == announcementId) {
                announcements.remove(i);
                isDeleted = true;
                break;
            }
        }
        if (!isDeleted) {
            return ServiceResponse.error("announcement_not_found");
        }

        group.setAnnouncements(announcements);

        try {
            contentRepository.save(group);
        } catch (DataIntegrityViolationException e) {
            return logAndReturnError("Failed to delete an announcement due to a database error when saving the group.", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while deleting an announcement.", "unexpected_error", e);
        }

        return ServiceResponse.value(true);
    }

    private <T> ServiceResponse<T> logAndReturnError(String message, String errorCode) {
        logger.error(message);
        return ServiceResponse.error(errorCode);
    }

    private <T> ServiceResponse<T> logAndReturnError(String message, String errorCode, Exception e) {
        logger.error(message, e);
        return ServiceResponse.error(errorCode);
    }
}

