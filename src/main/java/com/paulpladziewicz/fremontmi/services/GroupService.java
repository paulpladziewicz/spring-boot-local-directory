package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Group;
import com.paulpladziewicz.fremontmi.models.ServiceResponse;
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

@Service
public class GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
    private final GroupRepository groupRepository;
    private final UserService userService;

    public GroupService(GroupRepository groupRepository, UserService userService) {
        this.groupRepository = groupRepository;
        this.userService = userService;
    }

    @Transactional
    public ServiceResponse<Group> addGroup(Group group) {
        try {
            Optional<UserProfile> userProfileOpt = userService.getUserProfile();

            if (userProfileOpt.isEmpty()) {
                return logAndReturnError("Failed to add group: user profile not found.", "user_profile_not_found");
            }

            UserProfile userProfile = userProfileOpt.get();
            addUserToGroupAdministrators(group, userProfile);

            Group savedGroup = groupRepository.save(group);
            addGroupToUserProfile(savedGroup, userProfile);

            return ServiceResponse.value(savedGroup);

        } catch (DataAccessException e) {
            return logAndReturnError("Failed to save group due to a database error", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while creating group.", "unexpected_error", e);
        }
    }

    public ServiceResponse<List<Group>> findAll() {
        try {
            return ServiceResponse.value(groupRepository.findAll());
        } catch (DataAccessException e) {
            return logAndReturnError("Failed to retrieve groups due to a database error", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while retrieving groups.", "unexpected_error", e);
        }
    }

    public ServiceResponse<Group> findGroupById(String id) {
        try {
            Optional<Group> groupOpt = groupRepository.findById(id);
            return groupOpt.map(ServiceResponse::value)
                    .orElseGet(() -> logAndReturnError("Group not found with id: " + id, "group_not_found"));
        } catch (DataAccessException e) {
            return logAndReturnError("Failed to retrieve group with id " + id + " due to a database error", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while retrieving group with id " + id, "unexpected_error", e);
        }
    }

    public ServiceResponse<List<Group>> findGroupsByUser() {
        try {
            Optional<UserProfile> userProfileOpt = userService.getUserProfile();
            if (userProfileOpt.isEmpty()) {
                return logAndReturnError("Failed to retrieve groups: user profile not found.", "user_profile_not_found");
            }

            UserProfile userProfile = userProfileOpt.get();
            List<Group> groups = groupRepository.findAllById(userProfile.getGroupIds());
            return ServiceResponse.value(groups);

        } catch (DataAccessException e) {
            return logAndReturnError("Failed to retrieve groups for user due to a database error", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while retrieving groups for user.", "unexpected_error", e);
        }
    }

    @Transactional
    public ServiceResponse<Group> updateGroup(Group group) {
        try {
            ServiceResponse<UserProfile> userProfileResponse = getAndValidateUserProfileForAdmin(group.getId());
            if (userProfileResponse.hasError()) {
                return logAndReturnError("User doesn't have permission to update group", "permission_denied");
            }

            ServiceResponse<Group> groupResponse = findGroupById(group.getId());
            if (groupResponse.hasError()) {
                return groupResponse;
            }

            Group existingGroup = groupResponse.value();
            existingGroup.setName(group.getName());
            existingGroup.setDescription(group.getDescription());

            return ServiceResponse.value(groupRepository.save(existingGroup));

        } catch (DataAccessException e) {
            return logAndReturnError("Failed to update group due to a database error", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while updating group.", "unexpected_error", e);
        }
    }

    @Transactional
    public ServiceResponse<Void> deleteGroup(String groupId) {
        try {
            ServiceResponse<UserProfile> userProfileResponse = getAndValidateUserProfileForAdmin(groupId);
            if (userProfileResponse.hasError()) {
                return ServiceResponse.error(userProfileResponse.errorCode());
            }

            groupRepository.deleteById(groupId);
            logger.info("Successfully deleted group with id: {}", groupId);
            return ServiceResponse.value(null);

        } catch (DataAccessException e) {
            return logAndReturnError("Failed to delete group due to a database error", "database_error", e);
        } catch (Exception e) {
            return logAndReturnError("Unexpected error occurred while deleting group.", "unexpected_error", e);
        }
    }

    private ServiceResponse<UserProfile> getAndValidateUserProfileForAdmin(String groupId) {
        Optional<UserProfile> userProfileOpt = userService.getUserProfile();
        if (userProfileOpt.isEmpty()) {
            return logAndReturnError("User profile not found", "user_profile_not_found");
        }

        UserProfile userProfile = userProfileOpt.get();
        if (!userProfile.getGroupAdminIds().contains(groupId)) {
            return logAndReturnError("User is not an admin for group " + groupId, "permission_denied");
        }

        return ServiceResponse.value(userProfile);
    }

    @Transactional
    public ServiceResponse<Boolean> joinGroup(String groupId) {
        Optional<UserProfile> userProfileOptional = userService.getUserProfile();

        if (userProfileOptional.isEmpty()) {
            return logAndReturnError("User profile not found", "user_profile_not_found");
        }

        UserProfile userProfile = userProfileOptional.get();

        ServiceResponse<Group> serviceResponse = findGroupById(groupId);

        if (serviceResponse.hasError()) {
            return ServiceResponse.error(serviceResponse.errorCode());
        }

        Group group = serviceResponse.value();

        List<String> members = group.getMembers();
        if (!members.contains(userProfile.getUserId())) {
            members.add(userProfile.getUserId());
            group.setMembers(members);
            groupRepository.save(group);
        }
        List<String> groupIds = userProfile.getGroupIds();
        if (!groupIds.contains(groupId)) {
            groupIds.add(groupId);
            userProfile.setGroupIds(groupIds);
            userService.saveUserProfile(userProfile);
        }

        return ServiceResponse.value(true);
    }

    @Transactional
    public ServiceResponse<Boolean> leaveGroup(String groupId) {
        Optional<UserProfile> userProfileOptional =  userService.getUserProfile();

        if (userProfileOptional.isEmpty()) {
            return logAndReturnError("User profile not found", "user_profile_not_found");
        }

        UserProfile userProfile = userProfileOptional.get();

        ServiceResponse<Group> serviceResponse = findGroupById(groupId);

        if (serviceResponse.hasError()) {
            return ServiceResponse.error(serviceResponse.errorCode());
        }

        Group group = serviceResponse.value();

        List<String> members = new ArrayList<>(group.getMembers());
        members.remove(userProfile.getUserId());
        group.setMembers(members);
        Group savedGroup = groupRepository.save(group);

        List<String> groupIds = new ArrayList<>(userProfile.getGroupIds());
        groupIds.remove(savedGroup.getId());
        userProfile.setGroupIds(groupIds);
        userService.saveUserProfile(userProfile);

        return ServiceResponse.value(true);
    }

    private void addUserToGroupAdministrators(Group group, UserProfile userProfile) {
        group.getAdministrators().add(userProfile.getUserId());
        group.getMembers().add(userProfile.getUserId());
    }

    private void addGroupToUserProfile(Group savedGroup, UserProfile userProfile) {
        userProfile.getGroupIds().add(savedGroup.getId());
        userProfile.getGroupAdminIds().add(savedGroup.getId());
        userService.saveUserProfile(userProfile);
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

