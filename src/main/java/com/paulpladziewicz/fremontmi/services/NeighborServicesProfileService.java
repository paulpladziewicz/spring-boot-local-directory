package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.exceptions.ContentNotFoundException;
import com.paulpladziewicz.fremontmi.exceptions.PermissionDeniedException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class NeighborServicesProfileService {

    private static final Logger logger = LoggerFactory.getLogger(NeighborServicesProfileService.class);
    private final ContentRepository contentRepository;
    private final UserService userService;
    private final TagService tagService;
    private final SlugService slugService;
    private final EmailService emailService;
    private final UploadService uploadService;

    public NeighborServicesProfileService(ContentRepository contentRepository, UserService userService, SlugService slugService, TagService tagService, EmailService emailService, UploadService uploadService) {
        this.contentRepository = contentRepository;
        this.userService = userService;
        this.tagService = tagService;
        this.slugService = slugService;
        this.emailService = emailService;
        this.uploadService = uploadService;
    }

    public NeighborServicesProfile createNeighborServiceProfile(NeighborServicesProfile neighborServicesProfile) {
        UserProfile userProfile = userService.getUserProfile();

        neighborServicesProfile.setType(ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());
        neighborServicesProfile.setSlug(slugService.createUniqueSlug(neighborServicesProfile.getName(), ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType()));
        neighborServicesProfile.setPathname("/neighbor-services/" + neighborServicesProfile.getSlug());
        neighborServicesProfile.setVisibility(ContentVisibility.RESTRICTED.getVisibility());
        neighborServicesProfile.setStatus(ContentStatus.REQUIRES_ACTIVE_SUBSCRIPTION.getStatus());
        neighborServicesProfile.setCreatedBy(userProfile.getUserId());

        List<String> validatedTags = tagService.addTags(neighborServicesProfile.getTags(), ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());
        neighborServicesProfile.setTags(validatedTags);

        return contentRepository.save(neighborServicesProfile);
    }

    public List<NeighborServicesProfile> findAllActiveNeighborServices(String tag) {
        if (tag != null && !tag.isEmpty()) {
            return contentRepository.findByTagAndType(tag, ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType(), NeighborServicesProfile.class);
        } else {
            return contentRepository.findAllByType(ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType(), NeighborServicesProfile.class);
        }
    }

    public NeighborServicesProfile findNeighborServiceProfileById(String id) {
            return contentRepository.findById(id, NeighborServicesProfile.class)
                    .orElseThrow(() -> new ContentNotFoundException("NeighborServiceProfile with id '" + id + "' not found."));
    }

    public NeighborServicesProfile findNeighborServiceProfileBySlug(String slug) {
            return contentRepository.findBySlugAndType(slug, ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType(), NeighborServicesProfile.class)
                    .orElseThrow(() -> new ContentNotFoundException("Business with slug '" + slug + "' not found."));
    }

    public NeighborServicesProfile findNeighborServiceProfileByUserId() {
        String userId = userService.getUserId();

        return contentRepository.findByCreatedBy(userId, NeighborServicesProfile.class)
                .orElseThrow(() -> new ContentNotFoundException("NeighborServiceProfile with user id '" + userId + "' not found."));
    }

    @Transactional
    public NeighborServicesProfile updateNeighborServiceProfile(NeighborServicesProfile updatedProfile) {
        String userId = userService.getUserId();

        NeighborServicesProfile existingProfile = findNeighborServiceProfileById(updatedProfile.getId());

        checkPermission(userId, existingProfile);

        List<String> oldTags = existingProfile.getTags();
        List<String> newTags = updatedProfile.getTags();

        if (newTags != null) {
            tagService.updateTags(newTags, oldTags != null ? oldTags : new ArrayList<>(), ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());
        }

        if (!existingProfile.getName().equals(updatedProfile.getName())) {
            String newSlug = slugService.createUniqueSlug(updatedProfile.getName(), ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());
            existingProfile.setSlug(newSlug);
            existingProfile.setPathname("/neighbor-services/" + newSlug);
        }

        updateExistingNeighborServiceProfile(existingProfile, updatedProfile);

        return contentRepository.save(existingProfile);
    }

    @Transactional
    public void deleteNeighborService(String neighborServiceId) {
        String userId = userService.getUserId();

        NeighborServicesProfile neighborServicesProfile = findNeighborServiceProfileById(neighborServiceId);

        checkPermission(userId, neighborServicesProfile);

        tagService.removeTags(neighborServicesProfile.getTags(), ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());

        contentRepository.deleteById(neighborServiceId);
    }

    private void checkPermission(String userId, NeighborServicesProfile neighborServicesProfile) {
        if (!neighborServicesProfile.getCreatedBy().equals(userId)) {
            throw new PermissionDeniedException("User does not have permission to modify this event.");
        }
    }

    public boolean hasUploadPermission(String contentId) {
        String userId = userService.getUserId();
        NeighborServicesProfile neighborServicesProfile = findNeighborServiceProfileById(contentId);

        return neighborServicesProfile.getCreatedBy().equals(userId);
    }

    private void updateExistingNeighborServiceProfile(NeighborServicesProfile existingProfile, NeighborServicesProfile updatedProfile) {
        existingProfile.setName(updatedProfile.getName());
        existingProfile.setDescription(updatedProfile.getDescription());
        existingProfile.setEmail(updatedProfile.getEmail());
        existingProfile.setTags(updatedProfile.getTags());

        if (updatedProfile.getTags() != null && !updatedProfile.getTags().isEmpty()) {
            existingProfile.setTags(updatedProfile.getTags()); // Update the existing business's tags
        }

        if (updatedProfile.getNeighborServices() != null) {
            existingProfile.setNeighborServices(updatedProfile.getNeighborServices());
        }

        existingProfile.setUpdatedAt(LocalDateTime.now());
    }

    public void handleContactFormSubmission(String id, String name, String email, String message) {
        NeighborServicesProfile neighborServicesProfile = findNeighborServiceProfileById(id);

        emailService.sendContactNeighborServiceProfileEmail(neighborServicesProfile.getEmail(), name, email, message);
    }

    public void setProfileImageUrl(String contentId, String cdnUrl) {
        NeighborServicesProfile existingProfile = findNeighborServiceProfileById(contentId);

        if (existingProfile.getProfileImageUrl() != null) {
            existingProfile.getProfileImageUrls().add(existingProfile.getProfileImageUrl());
        }

        existingProfile.setProfileImageUrl(cdnUrl);
        contentRepository.save(existingProfile);

        removeOldImagesFromS3(existingProfile);
    }

    private void removeOldImagesFromS3(NeighborServicesProfile profile) {
        List<String> imageUrls = profile.getProfileImageUrls();

        for (String imageUrl : imageUrls) {
            uploadService.deleteFile(imageUrl);
        }

        profile.getProfileImageUrls().clear();
    }
}
