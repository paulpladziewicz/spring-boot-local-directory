package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Announcement;
import com.paulpladziewicz.fremontmi.models.Group;
import com.paulpladziewicz.fremontmi.models.UserProfile;
import com.paulpladziewicz.fremontmi.repositories.GroupRepository;
import com.paulpladziewicz.fremontmi.repositories.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;

    private final UserService userService;

    private final UserProfileRepository userProfileRepository;

    private final EmailService emailService;

    public GroupService(GroupRepository groupRepository, UserService userService, UserProfileRepository userProfileRepository, EmailService emailService) {
        this.groupRepository = groupRepository;
        this.userService = userService;
        this.userProfileRepository = userProfileRepository;
        this.emailService = emailService;
    }

    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    public Group findGroupById(String id) {
        return groupRepository.findById(id).orElse(null);
    }

    public List<Group> findGroupsByUser() {
        UserProfile userDetails = userService.getUserProfile();

        return groupRepository.findAllById(userDetails.getGroupIds());
    }

    @Transactional
    public Group addGroup(Group group) {
        UserProfile userDetails = userService.getUserProfile();

        List<String> administrators = new ArrayList<>(group.getAdministrators());
        List<String> members = new ArrayList<>(group.getMembers());
        administrators.add(userDetails.getUserId());
        members.add(userDetails.getUserId());
        group.setAdministrators(administrators);
        group.setMembers(members);
        Group savedGroup = groupRepository.save(group);

        List<String> groupIds = new ArrayList<>(userDetails.getGroupIds());
        List<String> adminGroupIds = new ArrayList<>(userDetails.getGroupAdminIds());
        groupIds.add(savedGroup.getId());
        adminGroupIds.add(savedGroup.getId());
        userDetails.setGroupIds(groupIds);
        userDetails.setGroupAdminIds(adminGroupIds);
        userProfileRepository.save(userDetails);

        return savedGroup;
    }

    @Transactional
    public void joinGroup(String groupId) {
        UserProfile userDetails = userService.getUserProfile();
        Group group = findGroupById(groupId);

        List<String> members = group.getMembers();
        if (!members.contains(userDetails.getUserId())) {
            members.add(userDetails.getUserId());
            group.setMembers(members);
            groupRepository.save(group);
        }

        List<String> groupIds = userDetails.getGroupIds();
        if (!groupIds.contains(groupId)) {
            groupIds.add(groupId);
            userDetails.setGroupIds(groupIds);
            userService.saveUserDetails(userDetails);
        }
    }

    @Transactional
    public void leaveGroup(String groupId) {
        UserProfile userDetails = userService.getUserProfile();
        Group group = findGroupById(groupId);

        List<String> members = new ArrayList<>(group.getMembers());
        members.remove(userDetails.getUserId());
        group.setMembers(members);
        Group savedGroup = groupRepository.save(group);

        List<String> groupIds = new ArrayList<>(userDetails.getGroupIds());
        groupIds.remove(savedGroup.getId());
        userDetails.setGroupIds(groupIds);
        userService.saveUserDetails(userDetails);
    }

    public Group updateGroup(Group group) {
        UserProfile userDetails = userService.getUserProfile();
        if (!userDetails.getGroupAdminIds().contains(group.getId())) {
            throw new RuntimeException("User doesn't have permission to update group");
        }
        Group groupDocument = findGroupById(group.getId());
        groupDocument.setName(group.getName());
        groupDocument.setDescription(group.getDescription());
        return groupRepository.save(groupDocument);
    }

    public void deleteGroup(String groupId) {
        UserProfile userDetails = userService.getUserProfile();
        if (!userDetails.getGroupAdminIds().contains(groupId)) {
            throw new RuntimeException("User doesn't have permission to delete group");
        }
        groupRepository.deleteById(groupId);
    }

    public List<Announcement> addAnnouncement(String groupId, Announcement announcement) {
        UserProfile userDetails = userService.getUserProfile();
        if (!userDetails.getGroupAdminIds().contains(groupId)) {
            throw new RuntimeException("User doesn't have permission to add an announcement to this group");
        }

        Group group = findGroupById(groupId);
        announcement.setId(group.getAnnouncements().size() + 1);
        List<Announcement> announcements = new ArrayList<>(group.getAnnouncements());
        announcements.addFirst(announcement);
        group.setAnnouncements(announcements);
        groupRepository.save(group);
        return announcements;
    }

    public boolean deleteAnnouncement(String groupId, int announcementId) {
        UserProfile userDetails = userService.getUserProfile();
        if (!userDetails.getGroupAdminIds().contains(groupId)) {
            throw new RuntimeException("User doesn't have permission to delete an announcement from this group");
        }

        Group group = findGroupById(groupId);
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
            return false;
        }

        group.setAnnouncements(announcements);

        groupRepository.save(group);

        return true;
    }

    public boolean emailGroup(String emailTarget, String groupId, String subject, String message) {
        Group group = findGroupById(groupId);

        if (group == null) {
            throw new IllegalArgumentException("Group not found");
        }

        List<String> userIds;
        if ("administrators".equalsIgnoreCase(emailTarget)) {
            userIds = group.getAdministrators();
        } else if ("members".equalsIgnoreCase(emailTarget)) {
            userIds = group.getMembers();
        } else {
            throw new IllegalArgumentException("Invalid email target: " + emailTarget);
        }

        List<String> emailAddresses = userProfileRepository.findAllById(userIds).stream()
                .map(UserProfile::getEmail)
                .collect(Collectors.toList());

        String replyToEmail = userService.getUserProfile().getEmail();

        try {
            emailService.sendGroupEmailAsync(emailAddresses, replyToEmail, subject, message);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
