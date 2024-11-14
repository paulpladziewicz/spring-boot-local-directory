package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.exceptions.ContentNotFoundException;
import com.paulpladziewicz.fremontmi.exceptions.PermissionDeniedException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ContentService {

    private final ContentRepository contentRepository;
    private final UserService userService;
    private final TagService tagService;

    public ContentService(ContentRepository contentRepository, UserService userService, TagService tagService) {
        this.contentRepository = contentRepository;
        this.userService = userService;
        this.tagService = tagService;
    }

    public Content create(ContentType type, ContentDto contentValues) {
        UserProfile userProfile = userService.getUserProfile();

        Content content = new Content();
        content.setType(type);
        content.setDetail(type);
        content.getDetail().update(content, contentValues);
        List<String> validatedTags = tagService.addTags(contentValues.getTags(), content.getType());
        content.setTags(validatedTags);
        content.setPathname(createUniquePathname(content.getDetail().getTitle(), type));
        content.setExternal(contentValues.isExternal());
        content.setNearby(contentValues.isNearby());
        content.setCreatedBy(userProfile.getUserId());
        content.setParticipants(Set.of(userProfile.getUserId()));
        content.setAdministrators(Set.of(userProfile.getUserId()));
        content.setCreatedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());

        content = contentRepository.save(content);

        userProfile.getContentActions()
                .computeIfAbsent(type, k -> new HashMap<>())
                .computeIfAbsent(ContentAction.CREATED, k -> new HashSet<>())
                .add(content.getId());

        userService.saveUserProfile(userProfile);

        return content;
    }

    public Content save(Content content) {
        return contentRepository.save(content);
    }

    public Content findById(String contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Content not found"));
    }

    public List<Content> findByArrayOfIds(List<String> contentIds) {
        return contentRepository.findByIdIn(contentIds);
    }

    public Content findByPathname(String pathname, ContentType type) {
        return contentRepository.findByPathname(pathname, type)
                .orElseThrow(() -> new ContentNotFoundException("Content not found with pathname: " + pathname + " and type: " + type));
    }

    public Page<Content> findByType(ContentType type, int page) {
        Pageable pageable = PageRequest.of(page, 9);
        return contentRepository.findByTypeAndVisibility(type, ContentVisibility.PUBLIC, pageable);
    }

    public Page<Content> findByTagAndType(String tag, ContentType type, int page) {
        Pageable pageable = PageRequest.of(page, 9);
        return contentRepository.findByTypeVisibilityAndTag(type, ContentVisibility.PUBLIC, tag, pageable);
    }

    public List<Content> findByUserAndType(ContentType contentType) {
        UserProfile userProfile = userService.getUserProfile();
        Map<ContentAction, Set<String>> contentActionsByType = userProfile.getContentActions().getOrDefault(contentType, new HashMap<>());

        List<String> contentIds = contentActionsByType.values()
                .stream()
                .flatMap(Set::stream)
                .distinct()
                .collect(Collectors.toList());

        return contentRepository.findByIdIn(contentIds);
    }

    public Optional<Content> findByTypeAndUserCreatedBy(ContentType contentType) {
        return contentRepository.findByTypeAndUserCreatedBy(contentType, userService.getUserId());
    }

    public Page<Content> findEvents(int page) {
        LocalDateTime startOfToday = LocalDateTime.now().with(LocalTime.MIDNIGHT);
        Pageable pageable = PageRequest.of(page, 50);
        return contentRepository.findEventsAfterStartTime(startOfToday, pageable);
    }

    public List<String> getAllContentEntityUrls() {
        List<Content> publicContent = contentRepository.findAllPublicContentPathnames();
        String baseUrl = "https://fremontmi.com";

        return publicContent.stream()
                .map(content -> baseUrl + content.getPathname())
                .collect(Collectors.toList());
    }

    public Content update(ContentDto updatedContent) {
        Content content = findById(updatedContent.getContentId());
        checkPermission(content);
        updateMetadata(content, updatedContent);
        content.getDetail().update(content, updatedContent);
        content.setUpdatedBy(userService.getUserId());
        content.setUpdatedAt(LocalDateTime.now());
        return contentRepository.save(content);
    }

    public void delete(String contentId) {
        UserProfile userProfile = userService.getUserProfile();
        Content content = findById(contentId);
        checkPermission(content);
        tagService.removeTags(content.getTags(), content.getType());
        removeContentFromUserProfile(userProfile, content.getType(), ContentAction.CREATED, contentId);
        contentRepository.deleteById(contentId);
    }

    public void checkPermission(Content content) {
        if (userService.isAdmin()) {
            return;
        }

        String userId = userService.getUserId();
        if (!content.getAdministrators().contains(userId)) {
            throw new PermissionDeniedException("You do not have permission to access this resource");
        }
    }

    public void updateMetadata(Content content, ContentDto updatedContent) {
        List<String> oldTags = content.getTags();
        List<String> newTags = updatedContent.getTags();

        if (newTags != null) {
            List<String> updatedTags = tagService.updateTags(newTags, oldTags != null ? oldTags : new ArrayList<>(), content.getType());
            content.setTags(updatedTags);
        }

        if (!updatedContent.getTitle().equals(content.getDetail().getTitle())) {
            String newPathname = createUniquePathname(updatedContent.getTitle(), content.getType());
            content.setPathname(newPathname);
        }

        content.setNearby(updatedContent.isNearby());
    }

    public String createUniquePathname(String name, ContentType contentType) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String basePathname = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        basePathname = "/" + contentType.toHyphenatedString() + "/" + basePathname;

        List<Content> matchingPathnames = contentRepository.findByPathnameRegexAndType("^" + Pattern.quote(basePathname) + "(-\\d+)?$", contentType);

        return generatePathnameFromMatches(basePathname, matchingPathnames);
    }

    private String generatePathnameFromMatches(String basePathname, List<Content> matchingPathnames) {
        if (matchingPathnames.isEmpty()) {
            return basePathname;
        }

        Pattern pattern = Pattern.compile(Pattern.quote(basePathname) + "-(\\d+)$");
        int maxNumber = 0;
        boolean basePathnameExists = false;

        for (Content content : matchingPathnames) {
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

    private void removeContentFromUserProfile(UserProfile userProfile, ContentType contentType, ContentAction action, String contentId) {
        Map<ContentAction, Set<String>> actions = userProfile.getContentActions().get(contentType);

        if (actions != null && actions.containsKey(action)) {
            Set<String> contentIds = actions.get(action);
            contentIds.remove(contentId);

            if (contentIds.isEmpty()) {
                actions.remove(action);
            }
        }

        userService.saveUserProfile(userProfile);
    }
}


