package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Group;
import com.paulpladziewicz.fremontmi.models.GroupDetailsDto;
import com.paulpladziewicz.fremontmi.repositories.GroupRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    public List<GroupDetailsDto> findGroupsForUser(List<String> memberGroupIds, List<String> adminGroupIds) {
        List<String> allGroupIds = new ArrayList<>(memberGroupIds);
        allGroupIds.addAll(adminGroupIds);
        List<Group> groups = groupRepository.findAllById(allGroupIds);
        return groups.stream()
                .map(group -> {
                    GroupDetailsDto dto = new GroupDetailsDto();
                    dto.setGroup(group);
                    if (adminGroupIds.contains(group.getId())) {
                        dto.setUserRole("admin");
                    } else {
                        dto.setUserRole("member");
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Group addGroup(Group group) {
        return groupRepository.save(group);
    }

    public Group updateGroup(Group group) {
        return groupRepository.save(group);
    }

    public void deleteGroup(String id) {
        groupRepository.deleteById(id);
    }
}
