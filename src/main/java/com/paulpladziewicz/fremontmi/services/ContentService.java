package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.exceptions.ContentNotFoundException;
import com.paulpladziewicz.fremontmi.exceptions.PermissionDeniedException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContentService {

    private static final Logger logger = LoggerFactory.getLogger(ContentService.class);
    private final ContentRepository contentRepository;
    private final UserService userService;
    private final TagService tagService;
    private final EmailService emailService;

    public ContentService(ContentRepository contentRepository, UserService userService, TagService tagService, EmailService emailService) {
        this.contentRepository = contentRepository;
        this.userService = userService;
        this.tagService = tagService;
        this.emailService = emailService;
    }

    public <T extends ContentDetail<T>> Content<T> create(ContentType type, T detail) {
        UserProfile userProfile = userService.getUserProfile();
        Content<T> content = new Content<>();
        content.setType(type);
        content.setDetail(detail);
        content.setPathname("");
        content.setCreatedBy(userProfile.getUserId());
        content.setAdministrators(List.of(userProfile.getUserId()));
        content.setCreatedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());
        return contentRepository.save(content);
    }

    @SuppressWarnings("unchecked")
    public <T extends ContentDetail<T>> Content<T> findById(String contentId) {
        return (Content<T>) contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Content not found"));
    }

    public Content<?> findByPathname(String pathname) {
        return contentRepository.findByPathname(pathname)
                .orElseThrow(() -> new ContentNotFoundException("Content with pathname '" + pathname + "' not found."));
    }

    public List<Content<?>> findByTag(String tag) {
        return contentRepository.findByTag(tag);
    }

    public List<Content<?>> findByType(String type) {
        return contentRepository.findByType(type);
    }

    public List<Content<?>> findByTagAndType(String tag, String type) {
        return contentRepository.findByTagAndType(tag, type);
    }

    public List<Content<?>> findByUser() {
        String userId = userService.getUserId();
        return contentRepository.findByCreatedBy(userId);
    }

    public List<Content<?>> findByUserAndType(String type) {
        String userId = userService.getUserId();
        return contentRepository.findByCreatedByAndType(userId, type);
    }

    public List<Content<?>> findAll() {
        return contentRepository.findAll();
    }

    public Content<?> update(String contentId, ContentDetail<?> updatedDetail) {
        Content<?> content = findById(contentId);
        checkPermission(content);
        content = castContent(content, updatedDetail);
        updateMetadata(content, updatedDetail);
        content.getDetail().update(content, updatedDetail);
        content.setUpdatedBy(userService.getUserId());
        content.setUpdatedAt(LocalDateTime.now());
        return contentRepository.save(content);
    }

    public Content<?> update(String contentId, UpdateType updateType, Map<String, Object> updateData) {
        Content<?> content = findById(contentId);
        checkPermission(content);
        content.getDetail().update(updateType, updateData);
        content.setUpdatedBy(userService.getUserId());
        content.setUpdatedAt(LocalDateTime.now());
        return contentRepository.save(content);
    }

    public void delete(String contentId) {
        UserProfile userProfile = userService.getUserProfile();
        Content<?> content = findById(contentId);
        checkPermission(content);
        tagService.removeTags(content.getDetail().getTags(), content.getType());
        contentRepository.deleteById(contentId);
    }

    public void heart(String contentId) {
        Content<?> content = findById(contentId);
        String userId = userService.getUserId();
        boolean alreadyHearted = content.getHeartedUserIds().contains(userId);

        if (alreadyHearted) {
            content.getHeartedUserIds().remove(userId);
            content.setHeartCount(content.getHeartCount() - 1);
        } else {
            content.getHeartedUserIds().add(userId);
            content.setHeartCount(content.getHeartCount() + 1);
        }

        contentRepository.save(content);
    }

    public void bookmark(String contentId) {
        Content<?> content = findById(contentId);
        //userService.bookmarkContent(contentId);
    }

    private void checkPermission(Content<?> content) {
        if (userService.isAdmin()) {
            return;
        }

        String userId = userService.getUserId();
        if (!content.getAdministrators().contains(userId)) {
            throw new PermissionDeniedException("You do not have permission to access this resource");
        }
    }

    public void updateMetadata(Content<?> content, ContentDetail<?> updatedDetail) {
        List<String> oldTags = content.getDetail().getTags();
        List<String> newTags = updatedDetail.getTags();

        if (newTags != null) {
            tagService.updateTags(newTags, oldTags != null ? oldTags : new ArrayList<>(), content.getType());
        }

        if (!updatedDetail.getName().equals(content.getDetail().getName())) {
            String newPathname = createUniquePathname(updatedDetail.getName(), content.getType());
            content.setPathname(newPathname);
        }
    }

    public String createUniquePathname(String name, ContentType contentType) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String basePathname = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        // Here, you might want to prepend the content type for structure, e.g., /events/music-festival
        basePathname = "/" + contentType.getContentType() + "/" + basePathname;

        List<Content<?>> matchingPathnames = contentRepository.findByPathnameRegexAndType("^" + Pattern.quote(basePathname) + "(-\\d+)?$", contentType);

        return generatePathnameFromMatches(basePathname, matchingPathnames);
    }

    private String generatePathnameFromMatches(String basePathname, List<Content<?>> matchingPathnames) {
        if (matchingPathnames.isEmpty()) {
            return basePathname;
        }

        Pattern pattern = Pattern.compile(Pattern.quote(basePathname) + "-(\\d+)$");
        int maxNumber = 0;
        boolean basePathnameExists = false;

        for (Content<?> content : matchingPathnames) {
            String pathname = content.getPathname();
            if (pathname.equals(basePathname)) {
                basePathnameExists = true;
            }

            Matcher matcher = pattern.matcher(pathname);
            if (matcher.find()) {
                maxNumber = Math.max(maxNumber, Integer.parseInt(matcher.group(1)));
            }
        }

        if (basePathnameExists || maxNumber > 0) {
            return basePathname + "-" + (maxNumber + 1);
        }

        return basePathname;
    }

    @SuppressWarnings("unchecked")
    private <T extends ContentDetail<T>> Content<T> castContent(Content<?> content, T detail) {
        return (Content<T>) content;
    }
}

