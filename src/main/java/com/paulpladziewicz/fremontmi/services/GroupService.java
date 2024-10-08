package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.exceptions.ContentNotFoundException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.AbstractDocument;
import java.text.Normalizer;
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

    private final TagService tagService;

    private final EmailService emailService;

    public GroupService(ContentRepository contentRepository, UserService userService, TagService tagService, EmailService emailService) {
        this.contentRepository = contentRepository;
        this.userService = userService;
        this.tagService = tagService;
        this.emailService = emailService;
    }

    @Transactional
    public ServiceResponse<Group> createGroup(Group group) {
        UserProfile userProfile = userService.getUserProfile();

        group.setMembers(List.of(userProfile.getUserId()));
        group.setAdministrators(List.of(userProfile.getUserId()));
        group.setType(ContentTypes.GROUP.getContentType());
        group.setSlug(createUniqueSlug(group.getName()));
        group.setPathname("/groups/" + group.getSlug());
        group.setCreatedBy(userProfile.getUserId());

        List<String> validatedTags = tagService.addTags(group.getTags(), ContentTypes.GROUP.getContentType());
        group.setTags(validatedTags);

        Group savedGroup = contentRepository.save(group);

        userProfile.getGroupIds().add(savedGroup.getId());
        userService.saveUserProfile(userProfile);

        return ServiceResponse.value(savedGroup);
    }

    public String createUniqueSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String baseSlug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        List<Content> matchingSlugs = contentRepository.findBySlugRegexAndType("^" + baseSlug + "(-\\d+)?$", ContentTypes.GROUP.getContentType());

        if (matchingSlugs.isEmpty()) {
            return baseSlug;
        }

        Pattern pattern = Pattern.compile(Pattern.quote(baseSlug) + "-(\\d+)$");

        int maxNumber = 0;
        boolean baseSlugExists = false;

        for (Content content : matchingSlugs) {
            String slug = content.getSlug();

            if (slug.equals(baseSlug)) {
                baseSlugExists = true;
            }

            Matcher matcher = pattern.matcher(slug);
            if (matcher.find()) {
                int number = Integer.parseInt(matcher.group(1));
                maxNumber = Math.max(maxNumber, number);
            }
        }

        if (baseSlugExists) {
            return baseSlug + "-" + (maxNumber + 1);
        } else if (maxNumber > 0) {
            return baseSlug + "-" + (maxNumber + 1);
        } else {
            return baseSlug;
        }
    }

    public Group saveGroup(Group group) {
        return contentRepository.save(group);
    }

    public List<Group> findAll(String tag) {
        if (tag != null && !tag.isEmpty()) {
            return contentRepository.findByTagAndType(tag, ContentTypes.GROUP.getContentType(), Group.class);
        } else {
            return contentRepository.findAllByType(ContentTypes.GROUP.getContentType(), Group.class);
        }
    }

    public Group findBySlug(String slug) {
        return contentRepository.findBySlugAndType(slug, ContentTypes.GROUP.getContentType(), Group.class)
                .orElseThrow(() -> new ContentNotFoundException("Group with slug '" + slug + "' not found."));
    }

    public Group findGroupById(String id) {
        return contentRepository.findById(id, Group.class)
                .orElseThrow(() -> new ContentNotFoundException("Group with id '" + id + "' not found."));
    }

    public ServiceResponse<List<Group>> findGroupsByUser() {
        UserProfile userProfile = userService.getUserProfile();
        List<Content> contentList = contentRepository.findAllById(userProfile.getGroupIds());

        List<Group> groups = contentList.stream()
                .filter(content -> content instanceof Group)
                .map(content -> (Group) content)
                .collect(Collectors.toList());

        return ServiceResponse.value(groups);
    }

    @Transactional
    public ServiceResponse<Content> updateGroup(Group group) {
        UserProfile userProfile = userService.getUserProfile();

        Group existingGroup = findGroupById(group.getId());

        if (!hasPermission(userProfile, existingGroup)) {
            return logAndReturnError("User doesn't have permission to delete group", "permission_denied");
        }

        List<String> existingTags = existingGroup.getTags();
        List<String> newTags = group.getTags();
        tagService.updateTags(newTags, existingTags, ContentTypes.GROUP.getContentType());

        if (!existingGroup.getName().equals(group.getName())) {
            String newSlug = createUniqueSlug(group.getName());
            existingGroup.setSlug(newSlug);
            existingGroup.setPathname("/groups/" + newSlug);
        }

        existingGroup.setName(group.getName());
        existingGroup.setDescription(group.getDescription());
        existingGroup.setTags(newTags);

        return ServiceResponse.value(contentRepository.save(existingGroup));
    }

    @Transactional
    public ServiceResponse<Void> deleteGroup(String groupId) {
        UserProfile userProfile = userService.getUserProfile();

        Group groupContent = findGroupById(groupId);

        if (!hasPermission(userProfile, groupContent)) {
            return logAndReturnError("User doesn't have permission to delete group", "permission_denied");
        }

        tagService.removeTags(groupContent.getTags(), ContentTypes.GROUP.getContentType());

        List<String> groupIds = userProfile.getGroupIds();
        groupIds.remove(groupId);
        userProfile.setGroupIds(groupIds);
        userService.saveUserProfile(userProfile);

        contentRepository.deleteById(groupId);
        logger.info("Successfully deleted group with content id: {}", groupId);
        return ServiceResponse.value(null);
    }

    private Boolean hasPermission(UserProfile userProfile, Group group) {
        return group.getAdministrators().contains(userProfile.getUserId());
    }

    @Transactional
    public ServiceResponse<Boolean> joinGroup(String slug) {
        UserProfile userProfile = userService.getUserProfile();

        Group group = findBySlug(slug);

        List<String> members = group.getMembers();
        if (!members.contains(userProfile.getUserId())) {
            members.add(userProfile.getUserId());
            group.setMembers(members);
            contentRepository.save(group);
        }
        List<String> groupIds = userProfile.getGroupIds();
        if (!groupIds.contains(group.getId())) {
            groupIds.add(group.getId());
            userProfile.setGroupIds(groupIds);
            userService.saveUserProfile(userProfile);
        }

        return ServiceResponse.value(true);
    }

    @Transactional
    public ServiceResponse<Boolean> leaveGroup(String slug) {
        UserProfile userProfile = userService.getUserProfile();

        Group group = findBySlug(slug);

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

    public ServiceResponse<List<Announcement>> addAnnouncement(String groupId, Announcement announcement) {
        UserProfile userProfile = userService.getUserProfile();

        Group group = findGroupById(groupId);

        if (!hasPermission(userProfile, group)) {
            return logAndReturnError("User doesn't have permission to delete group", "permission_denied");
        }

        announcement.setId(group.getAnnouncements().size() + 1);
        List<Announcement> announcements = new ArrayList<>(group.getAnnouncements());
        announcements.addFirst(announcement);
        group.setAnnouncements(announcements);

        contentRepository.save(group);


        return ServiceResponse.value(announcements);
    }

    public ServiceResponse<Boolean> deleteAnnouncement(String id, int announcementId) {
        UserProfile userProfile = userService.getUserProfile();

        Group group = findGroupById(id);

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

        contentRepository.save(group);

        return ServiceResponse.value(true);
    }

    public ServiceResponse<Boolean> emailGroup(String slug, String subject, String message) {
        UserProfile senderUserProfile = userService.getUserProfile();

        if (senderUserProfile.getEmailSendCount() >= 5) {
            return logAndReturnError("User has reached the email limit", "email_limit_reached");
        }

        senderUserProfile.setEmailSendCount(senderUserProfile.getEmailSendCount() + 1);

        userService.saveUserProfile(senderUserProfile);

        Optional<Group> optionalGroup = contentRepository.findBySlugAndType(slug, ContentTypes.GROUP.getContentType())
                .filter(content -> content instanceof Group)
                .map(content -> (Group) content);

        if (optionalGroup.isEmpty()) {
            return logAndReturnError("Group not found with slug: " + slug, "group_not_found");
        }

        Group group = optionalGroup.get();

        if (group.getAdministrators().contains(senderUserProfile.getUserId())) {
            List<UserProfile> memberUserProfiles = userService.getUserProfiles(group.getMembers());

            List<String> emailAddresses = memberUserProfiles.stream()
                    .map(UserProfile::getEmail)
                    .filter(email -> email != null && !email.isEmpty())
                    .collect(Collectors.toList());

            return ServiceResponse.value(emailService.sendGroupEmail(emailAddresses, senderUserProfile.getEmail(), senderUserProfile.getFirstName() + ' ' + senderUserProfile.getLastName(), group.getName(), subject, message));
        }

        if (group.getMembers().contains(senderUserProfile.getUserId())) {
            List<UserProfile> adminUserProfiles = userService.getUserProfiles(group.getAdministrators());

            List<String> emailAddresses = adminUserProfiles.stream()
                    .map(UserProfile::getEmail)
                    .filter(email -> email != null && !email.isEmpty())
                    .collect(Collectors.toList());

            return ServiceResponse.value(emailService.sendGroupEmail(emailAddresses, senderUserProfile.getEmail(), senderUserProfile.getFirstName() + ' ' + senderUserProfile.getLastName(), group.getName(), subject, message));
        }

        return logAndReturnError("User doesn't have permission to email group", "permission_denied");
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

