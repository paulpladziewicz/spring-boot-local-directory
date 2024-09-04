package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Announcement;
import com.paulpladziewicz.fremontmi.models.Group;
import com.paulpladziewicz.fremontmi.models.ServiceResult;
import com.paulpladziewicz.fremontmi.models.UserProfile;
import com.paulpladziewicz.fremontmi.repositories.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

    private final GroupRepository groupRepository;

    private final UserService userService;

    private final EmailService emailService;

    public GroupService(GroupRepository groupRepository, UserService userService, EmailService emailService) {
        this.groupRepository = groupRepository;
        this.userService = userService;
        this.emailService = emailService;
    }

    @Transactional
    public ServiceResult<Group> addGroup(Group group) {
        try {
            Optional<UserProfile> userProfile = userService.getUserProfile();

            if (userProfile.isEmpty()) {
                logger.error("Failed to add group: user profile not found for the user creating the group.");
                return ServiceResult.error("Failed to add group: user profile not found.", "user_profile_not_found");
            }

            UserProfile userProfileData = userProfile.get();

            List<String> administrators = new ArrayList<>(group.getAdministrators());
            List<String> members = new ArrayList<>(group.getMembers());
            administrators.add(userProfileData.getUserId());
            members.add(userProfileData.getUserId());
            group.setAdministrators(administrators);
            group.setMembers(members);

            Group savedGroup = groupRepository.save(group);

            List<String> groupIds = new ArrayList<>(userProfileData.getGroupIds());
            List<String> adminGroupIds = new ArrayList<>(userProfileData.getGroupAdminIds());
            groupIds.add(savedGroup.getId());
            adminGroupIds.add(savedGroup.getId());
            userProfileData.setGroupIds(groupIds);
            userProfileData.setGroupAdminIds(adminGroupIds);

            ServiceResult<Void> saveProfileResult = userService.saveUserProfile(userProfileData);

            if (!saveProfileResult.isSuccess()) {
                logger.error("Failed to add group: could not save user profile.");
                return ServiceResult.error("Failed to add group: could not save user profile.", saveProfileResult.getErrorCode());
            }

            return ServiceResult.success(savedGroup);
        } catch (DataAccessException e) {
            logger.error("Failed to save group due to database error", e);
            return ServiceResult.error("Failed to save group due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while creating group.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    public ServiceResult<List<Group>> findAll() {
        System.out.println("calling group service");
        try {
            List<Group> groups = groupRepository.findAll();
            return ServiceResult.success(groups);
        } catch (DataAccessException e) {
            logger.error("Failed to retrieve groups due to a database error", e);
            return ServiceResult.error("Failed to retrieve groups due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while retrieving groups.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    public ServiceResult<Group> findGroupById(String id) {
        System.out.println("calling");
        try {
            Optional<Group> group = groupRepository.findById(id);
            if (group.isPresent()) {
                return ServiceResult.success(group.get());
            } else {
                logger.warn("Group not found with id: {}", id);
                return ServiceResult.error("Group not found.", "group_not_found");
            }
        } catch (DataAccessException e) {
            logger.error("Failed to retrieve group with id {} due to a database error", id, e);
            return ServiceResult.error("Failed to retrieve group due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while retrieving group with id {}.", id, e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    public ServiceResult<List<Group>> findGroupsByUser() {
        try {
            Optional<UserProfile> userProfile = userService.getUserProfile();

            if (userProfile.isEmpty()) {
                logger.error("Failed to retrieve groups: could not retrieve user profile.");
                return ServiceResult.error("Failed to retrieve groups: could not retrieve user profile.", "user_profile_not_found");
            }

            UserProfile userProfileData = userProfile.get();
            List<Group> groups = groupRepository.findAllById(userProfileData.getGroupIds());

            return ServiceResult.success(groups);
        } catch (DataAccessException e) {
            logger.error("Failed to retrieve groups for user due to a database error", e);
            return ServiceResult.error("Failed to retrieve groups due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while retrieving groups for user.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    @Transactional
    public ServiceResult<Group> updateGroup(Group group) {
        try {
            Optional<UserProfile> userProfile = userService.getUserProfile();

            if (userProfile.isEmpty()) {
                logger.error("Failed to update group: could not retrieve user profile.");
                return ServiceResult.error("Failed to update group: could not retrieve user profile.", "user_profile_not_found");
            }

            UserProfile userProfileData = userProfile.get();

            if (!userProfileData.getGroupAdminIds().contains(group.getId())) {
                logger.warn("User doesn't have permission to update group with id: {}", group.getId());
                return ServiceResult.error("User doesn't have permission to update the group.", "permission_denied");
            }

            ServiceResult<Group> groupResult = findGroupById(group.getId());
            if (!groupResult.isSuccess()) {
                return groupResult;
            }

            Group groupDocument = groupResult.getData();
            groupDocument.setName(group.getName());
            groupDocument.setDescription(group.getDescription());

            Group updatedGroup = groupRepository.save(groupDocument);
            return ServiceResult.success(updatedGroup);

        } catch (DataAccessException e) {
            logger.error("Failed to update group due to a database error", e);
            return ServiceResult.error("Failed to update group due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while updating group.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }


    @Transactional
    public ServiceResult<Void> deleteGroup(String groupId) {
        try {
            Optional<UserProfile> userProfile = userService.getUserProfile();

            if (userProfile.isEmpty()) {
                logger.error("Failed to delete group: could not retrieve user profile.");
                return ServiceResult.error("Failed to delete group: could not retrieve user profile.", "user_profile_not_found");
            }

            UserProfile userProfileData = userProfile.get();

            if (!userProfileData.getGroupAdminIds().contains(groupId)) {
                logger.warn("User doesn't have permission to delete group with id: {}", groupId);
                return ServiceResult.error("User doesn't have permission to delete the group.", "permission_denied");
            }

            groupRepository.deleteById(groupId);
            logger.info("Successfully deleted group with id: {}", groupId);
            return ServiceResult.success();

        } catch (DataAccessException e) {
            logger.error("Failed to delete group due to a database error", e);
            return ServiceResult.error("Failed to delete group due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while deleting group.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    @Transactional
    public ServiceResult<Void> joinGroup(String groupId) {
        try {
            Optional<UserProfile> userProfile = userService.getUserProfile();

            if (userProfile.isEmpty()) {
                logger.error("Failed to join group: could not retrieve user profile.");
                return ServiceResult.error("Failed to join group: could not retrieve user profile.", "user_profile_not_found");
            }

            UserProfile userProfileData = userProfile.get();

            ServiceResult<Group> groupResult = findGroupById(groupId);
            if (!groupResult.isSuccess()) {
                return ServiceResult.error("Failed to join group due to a database error.", groupResult.getErrorCode());
            }

            Group group = groupResult.getData();

            List<String> members = group.getMembers();
            if (!members.contains(userProfileData.getUserId())) {
                members.add(userProfileData.getUserId());
                group.setMembers(members);
                groupRepository.save(group);
                logger.info("User {} joined group {}", userProfileData.getUserId(), groupId);
            }

            List<String> groupIds = userProfileData.getGroupIds();
            if (!groupIds.contains(groupId)) {
                groupIds.add(groupId);
                userProfileData.setGroupIds(groupIds);
                ServiceResult<Void> saveUserResult = userService.saveUserProfile(userProfileData);
                if (!saveUserResult.isSuccess()) {
                    logger.error("Failed to join group: could not save updated user profile.");
                    return ServiceResult.error("Failed to join group: could not save updated user profile.", saveUserResult.getErrorCode());
                }
            }

            return ServiceResult.success();

        } catch (DataAccessException e) {
            logger.error("Failed to join group due to a database error", e);
            return ServiceResult.error("Failed to join group due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while joining group.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }


    @Transactional
    public ServiceResult<Void> leaveGroup(String groupId) {
        try {
            Optional<UserProfile> userProfile = userService.getUserProfile();

            if (userProfile.isEmpty()) {
                logger.error("Failed to leave group: could not retrieve user profile.");
                return ServiceResult.error("Failed to leave group: could not retrieve user profile.", "user_profile_not_found");
            }

            UserProfile userProfileData = userProfile.get();

            ServiceResult<Group> groupResult = findGroupById(groupId);
            if (!groupResult.isSuccess()) {
                return ServiceResult.error("Failed to leave group due to a database error.", groupId);
            }

            Group group = groupResult.getData();

            List<String> members = new ArrayList<>(group.getMembers());
            if (!members.remove(userProfileData.getUserId())) {
                logger.warn("User {} is not a member of group {}", userProfileData.getUserId(), groupId);
                return ServiceResult.error("User is not a member of the group.", "not_a_member");
            }
            group.setMembers(members);
            groupRepository.save(group);
            logger.info("User {} left group {}", userProfileData.getUserId(), groupId);

            List<String> groupIds = new ArrayList<>(userProfileData.getGroupIds());
            if (!groupIds.remove(groupId)) {
                logger.warn("Group {} is not in the user's group list", groupId);
                return ServiceResult.error("Group is not in the user's group list.", "group_not_found_in_user_profile");
            }
            userProfileData.setGroupIds(groupIds);
            ServiceResult<Void> saveUserResult = userService.saveUserProfile(userProfileData);
            if (!saveUserResult.isSuccess()) {
                logger.error("Failed to leave group: could not save updated user profile.");
                return ServiceResult.error("Failed to leave group: could not save updated user profile.", saveUserResult.getErrorCode());
            }

            return ServiceResult.success();

        } catch (DataAccessException e) {
            logger.error("Failed to leave group due to a database error", e);
            return ServiceResult.error("Failed to leave group due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while leaving group.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    @Transactional
    public ServiceResult<List<Announcement>> addAnnouncement(String groupId, Announcement announcement) {
        try {
            Optional<UserProfile> userProfile = userService.getUserProfile();

            if (userProfile.isEmpty()) {
                logger.error("Failed to add announcement: could not retrieve user profile.");
                return ServiceResult.error("Failed to add announcement: could not retrieve user profile.", "user_profile_not_found");
            }

            UserProfile userProfileData = userProfile.get();

            if (!userProfileData.getGroupAdminIds().contains(groupId)) {
                logger.warn("User doesn't have permission to add an announcement to group with id: {}", groupId);
                return ServiceResult.error("User doesn't have permission to add an announcement to this group.", "permission_denied");
            }

            ServiceResult<Group> groupResult = findGroupById(groupId);
            if (!groupResult.isSuccess()) {
                return ServiceResult.error("Failed to add announcement: could not find group.", groupResult.getErrorCode());
            }

            Group group = groupResult.getData();

            announcement.setId(group.getAnnouncements().size() + 1);
            List<Announcement> announcements = new ArrayList<>(group.getAnnouncements());
            announcements.addFirst(announcement);
            group.setAnnouncements(announcements);

            groupRepository.save(group);
            logger.info("Announcement added to group {} by user {}", groupId, userProfileData.getUserId());

            return ServiceResult.success(announcements);

        } catch (DataAccessException e) {
            logger.error("Failed to add announcement due to a database error", e);
            return ServiceResult.error("Failed to add announcement due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while adding announcement.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }

    @Transactional
    public ServiceResult<Void> deleteAnnouncement(String groupId, int announcementId) {
        try {
            Optional<UserProfile> userProfile = userService.getUserProfile();

            if (userProfile.isEmpty()) {
                logger.error("Failed to delete announcement: could not retrieve user profile.");
                return ServiceResult.error("Failed to delete announcement: could not retrieve user profile.", "user_profile_not_found");
            }

            UserProfile userProfileData = userProfile.get();

            if (!userProfileData.getGroupAdminIds().contains(groupId)) {
                logger.warn("User doesn't have permission to delete an announcement from group with id: {}", groupId);
                return ServiceResult.error("User doesn't have permission to delete an announcement from this group.", "permission_denied");
            }

            ServiceResult<Group> groupResult = findGroupById(groupId);
            if (!groupResult.isSuccess()) {
                return ServiceResult.error("Failed to delete announcement: could not find group.", groupResult.getErrorCode());
            }

            Group group = groupResult.getData();
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
                logger.warn("Announcement with id {} not found in group {}", announcementId, groupId);
                return ServiceResult.error("Announcement not found in order to delete.", "announcement_not_found");
            }

            group.setAnnouncements(announcements);
            groupRepository.save(group);
            logger.info("Announcement with id {} deleted from group {}", announcementId, groupId);

            return ServiceResult.success();

        } catch (DataAccessException e) {
            logger.error("Failed to delete announcement due to a database error", e);
            return ServiceResult.error("Failed to delete announcement due to a database error. Please try again later.", "database_error");
        } catch (Exception e) {
            logger.error("Unexpected error occurred while deleting announcement.", e);
            return ServiceResult.error("An unexpected error occurred. Please try again later.", "unexpected_error");
        }
    }


    public ServiceResult<Void> emailGroup(String emailTarget, String groupId, String subject, String message) {
        try {
            ServiceResult<Group> groupResult = findGroupById(groupId);
            if (!groupResult.isSuccess()) {
                return ServiceResult.error("Could not find group by id in order to email group.", groupResult.getErrorCode());
            }

            Group group = groupResult.getData();

            List<String> userIds;
            if ("administrators".equalsIgnoreCase(emailTarget)) {
                userIds = group.getAdministrators();
            } else if ("members".equalsIgnoreCase(emailTarget)) {
                userIds = group.getMembers();
            } else {
                logger.error("Invalid email target: {}", emailTarget);
                return ServiceResult.error("Invalid email target: " + emailTarget, "invalid_email_target");
            }

            List<String> emailAddresses = userService.findAllById(userIds).stream()
                    .map(UserProfile::getEmail)
                    .collect(Collectors.toList());

            Optional<UserProfile> userProfileResult = userService.getUserProfile();
            if (userProfileResult.isEmpty()) {
                return ServiceResult.error("Could not retrieve user profile in order to email group.", "user_profile_not_found");
            }

            String replyToEmail = userProfileResult.get().getEmail();

            emailService.sendGroupEmailAsync(emailAddresses, replyToEmail, subject, message);

            return ServiceResult.success();
        } catch (Exception e) {
            logger.error("Failed to send group email", e);
            return ServiceResult.error("Failed to send group email due to an unexpected error. Please try again later.", "email_error");
        }
    }
}
