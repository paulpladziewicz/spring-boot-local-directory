package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Group;
import com.paulpladziewicz.fremontmi.models.GroupDetailsDto;
import com.paulpladziewicz.fremontmi.models.UserDetailsDto;
import com.paulpladziewicz.fremontmi.repositories.GroupRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;

    private final UserService userService;

    public GroupService(GroupRepository groupRepository, UserService userService, UserDetailsService userDetailsService) {
        this.groupRepository = groupRepository;
        this.userService = userService;
    }

    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    public Group findGroupById(String id) {
        return groupRepository.findById(id).orElse(null);
    }

    public List<GroupDetailsDto> findGroupsForUser() {
        Optional<UserDetailsDto> userDetails = userService.getUserDetails();

        if (userDetails.isEmpty()) {
            return Collections.emptyList();
        }

        System.out.println("Fetching groups for IDs: " + userDetails.get().getGroupIds());

        List<Group> groups = groupRepository.findAllById(userDetails.get().getGroupIds());
        System.out.println("Groups found: " + groups.size());

        return groups.stream()
                .map(group -> {
                    GroupDetailsDto dto = new GroupDetailsDto();
                    dto.setGroup(group);
                    if (userDetails.get().getGroupAdminIds().contains(group.getId())) {
                        dto.setUserRole("admin");
                    } else {
                        dto.setUserRole("member");
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Group addGroup (Group group) {
        Optional<UserDetailsDto> userDetailsOpt = userService.getUserDetails();

        if (!userDetailsOpt.isPresent()) {
            throw new IllegalStateException("User details not found");
        }

        UserDetailsDto userDetails = userDetailsOpt.get();

        // Ensure administrators and members are initialized to avoid NullPointerException
        List<String> administrators = new ArrayList<>(group.getAdministrators());
        List<String> members = new ArrayList<>(group.getMembers());

        // Add the current user's username to administrators and members of the group
        administrators.add(userDetails.getUsername());
        members.add(userDetails.getUsername());

        group.setAdministrators(administrators);
        group.setMembers(members);

        // Save the group and get the updated instance with ID populated
        Group savedGroup = groupRepository.save(group);

        // Update user details with the new group ID
        List<String> groupIds = new ArrayList<>(userDetails.getGroupIds());
        List<String> adminGroupIds = new ArrayList<>(userDetails.getGroupAdminIds());

        groupIds.add(savedGroup.getId());
        adminGroupIds.add(savedGroup.getId());

        userDetails.setGroupIds(groupIds);
        userDetails.setGroupAdminIds(adminGroupIds);

        userService.saveUserDetails(userDetails);

        return savedGroup;
    }

    public void joinGroup (String groupId) {
        Optional<UserDetailsDto> userDetailsOpt = userService.getUserDetails();
        Group group = findGroupById(groupId);

        if (!userDetailsOpt.isPresent()) {
            throw new IllegalStateException("User details not found");
        }

        UserDetailsDto userDetails = userDetailsOpt.get();

        List<String> members = new ArrayList<>(group.getMembers());

        members.add(userDetails.getUsername());

        group.setMembers(members);

        Group savedGroup = groupRepository.save(group);

        List<String> groupIds = new ArrayList<>(userDetails.getGroupIds());

        groupIds.add(savedGroup.getId());

        userDetails.setGroupIds(groupIds);

        userService.saveUserDetails(userDetails);
    }

    public Group updateGroup (Group group) {
        return groupRepository.save(group);
    }

    public void deleteGroup (String id) {
        groupRepository.deleteById(id);
    }
}
