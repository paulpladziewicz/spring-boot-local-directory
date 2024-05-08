package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Announcement;
import com.paulpladziewicz.fremontmi.models.Group;
import com.paulpladziewicz.fremontmi.services.GroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("/api/announcements")
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final GroupService groupService;

    public AnnouncementController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/{groupId}")
    public List<Announcement> getAllGroupAnnouncements(@NotNull @PathVariable String groupId) {
        Group group = groupService.findGroupById(groupId);
        return group.getAnnouncements();
    }

    @PostMapping("/{groupId}")
    public List<Announcement> addGroupAnnouncement(@NotNull @PathVariable String groupId, @Valid @RequestBody Announcement announcement, BindingResult result) {
        if (result.hasErrors()) {
            return (List<Announcement>) ResponseEntity.badRequest().body(result.getAllErrors());
        }
        return groupService.addAnnouncement(groupId, announcement);
    }

    @PutMapping("/{groupId}")
    public List<Announcement> updateGroupAnnouncement(@NotNull @PathVariable String groupId, @Valid @RequestBody Announcement announcement, BindingResult result) {
        if (result.hasErrors()) {
            return (List<Announcement>) ResponseEntity.badRequest().body(result.getAllErrors());
        }
        return groupService.updateAnnouncement(groupId, announcement);
    }
}
