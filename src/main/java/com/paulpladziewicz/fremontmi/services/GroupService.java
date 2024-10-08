package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.exceptions.ContentNotFoundException;
import com.paulpladziewicz.fremontmi.exceptions.PermissionDeniedException;
import com.paulpladziewicz.fremontmi.exceptions.ValidationException;
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
    public Group createGroup(Group group) {
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

        return savedGroup;
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

    public List<Group> findGroupsByUser() {
        UserProfile userProfile = userService.getUserProfile();
        List<Content> contentList = contentRepository.findAllById(userProfile.getGroupIds());

        return contentList.stream()
                .filter(content -> content instanceof Group)
                .map(content -> (Group) content)
                .collect(Collectors.toList());
    }

    @Transactional
    public Group updateGroup(Group group) {
        UserProfile userProfile = userService.getUserProfile();

        Group existingGroup = findGroupById(group.getId());

        if (!hasPermission(userProfile, existingGroup)) {
            throw new PermissionDeniedException("User doesn't have permission to update group.");
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

        return contentRepository.save(existingGroup);
    }

    @Transactional
    public void deleteGroup(String groupId) {
        UserProfile userProfile = userService.getUserProfile();

        Group groupContent = findGroupById(groupId);

        if (!hasPermission(userProfile, groupContent)) {
            throw new PermissionDeniedException("User doesn't have permission to delete group.");
        }

        tagService.removeTags(groupContent.getTags(), ContentTypes.GROUP.getContentType());

        List<String> groupIds = userProfile.getGroupIds();
        groupIds.remove(groupId);
        userProfile.setGroupIds(groupIds);
        userService.saveUserProfile(userProfile);

        contentRepository.deleteById(groupId);
    }

    private Boolean hasPermission(UserProfile userProfile, Group group) {
        return group.getAdministrators().contains(userProfile.getUserId());
    }

    @Transactional
    public void joinGroup(String slug) {
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
    }

    @Transactional
    public void leaveGroup(String slug) {
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
    }

    public List<Announcement> addAnnouncement(String groupId, Announcement announcement) {
        UserProfile userProfile = userService.getUserProfile();

        Group group = findGroupById(groupId);

        if (!hasPermission(userProfile, group)) {
            throw new PermissionDeniedException("User doesn't have permission to add an announcement.");
        }

        announcement.setId(group.getAnnouncements().size() + 1);
        List<Announcement> announcements = new ArrayList<>(group.getAnnouncements());
        announcements.addFirst(announcement);
        group.setAnnouncements(announcements);

        contentRepository.save(group);

        return announcements;
    }

    public void deleteAnnouncement(String id, int announcementId) {
        UserProfile userProfile = userService.getUserProfile();

        Group group = findGroupById(id);

        if (!hasPermission(userProfile, group)) {
            throw new PermissionDeniedException("User doesn't have permission to delete an announcement.");
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
            throw new ContentNotFoundException("Announcement not found.");
        }

        group.setAnnouncements(announcements);

        contentRepository.save(group);
    }

    public Boolean emailGroup(String slug, String subject, String message) {
        UserProfile senderUserProfile = userService.getUserProfile();

        if (senderUserProfile.getEmailSendCount() >= 5) {
            throw new ValidationException("User has reached the email limit.");
        }

        senderUserProfile.setEmailSendCount(senderUserProfile.getEmailSendCount() + 1);

        userService.saveUserProfile(senderUserProfile);

        Group group = findBySlug(slug);

        if (group.getAdministrators().contains(senderUserProfile.getUserId())) {
            List<UserProfile> memberUserProfiles = userService.getUserProfiles(group.getMembers());

            List<String> emailAddresses = memberUserProfiles.stream()
                    .map(UserProfile::getEmail)
                    .filter(email -> email != null && !email.isEmpty())
                    .collect(Collectors.toList());

            return emailService.sendGroupEmail(emailAddresses, senderUserProfile.getEmail(), senderUserProfile.getFirstName() + ' ' + senderUserProfile.getLastName(), group.getName(), subject, message);
        }

        if (group.getMembers().contains(senderUserProfile.getUserId())) {
            List<UserProfile> adminUserProfiles = userService.getUserProfiles(group.getAdministrators());

            List<String> emailAddresses = adminUserProfiles.stream()
                    .map(UserProfile::getEmail)
                    .filter(email -> email != null && !email.isEmpty())
                    .collect(Collectors.toList());

            return emailService.sendGroupEmail(emailAddresses, senderUserProfile.getEmail(), senderUserProfile.getFirstName() + ' ' + senderUserProfile.getLastName(), group.getName(), subject, message);
        }

        throw new PermissionDeniedException("User doesn't have permission to email the group.");
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
}

