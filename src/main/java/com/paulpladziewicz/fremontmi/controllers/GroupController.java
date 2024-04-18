package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Group;
import com.paulpladziewicz.fremontmi.services.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

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

    @GetMapping("/api/groups")
    @ResponseBody
    public List<Group> getAllGroups() {
        return groupService.findAll();
    }

    @PostMapping("/api/groups")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public Group createGroup(@Valid @RequestBody Group group) {
        return groupService.createGroup(group);
    }
}
