package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Group;
import com.paulpladziewicz.fremontmi.models.GroupDetailsDto;
import com.paulpladziewicz.fremontmi.services.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/groups")
    public String displayGroups(Model model) {
        model.addAttribute("groups", groupService.findAll());
        return "groups";
    }

    @GetMapping("/groups/{id}")
    public String displayGroup(@PathVariable String id, Model model) {
        model.addAttribute("id", id);
        return "group-page";
    }

    @GetMapping("/my/groups/{id}")
    public String displayMyGroup(@PathVariable String id, Model model) {
        model.addAttribute("id", id);
        return "dashboard";
    }

    @GetMapping("/api/groups")
    @ResponseBody
    public List<Group> getAllGroups() {
        return groupService.findAll();
    }

    @GetMapping("/api/groups/user")
    public ResponseEntity<List<GroupDetailsDto>> getUserGroups() {
        List<String> memberGroupIds = Arrays.asList("662aab87ebdea711e0a48635");
        List<String> adminGroupIds = Arrays.asList("662bc548d2d69a7da9ee6040");
        try {
            List<GroupDetailsDto> groupDetails = groupService.findGroupsForUser(memberGroupIds, adminGroupIds);
            return new ResponseEntity<>(groupDetails, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/api/groups")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public Group createGroup(@Valid @RequestBody Group group) {
        return groupService.addGroup(group);
    }

    @PutMapping("/api/groups")
    @ResponseBody
    public Group updateGroup(@Valid @RequestBody Group group) {
        return groupService.updateGroup(group);
    }

    @DeleteMapping("/api/groups/{id}")
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable String id) {
        groupService.deleteGroup(id);
    }
}
