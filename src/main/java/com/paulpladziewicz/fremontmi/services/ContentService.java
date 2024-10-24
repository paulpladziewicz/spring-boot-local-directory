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

    public Content create(ContentType type, ContentDetail detail) {
        UserProfile userProfile = userService.getUserProfile();
        Content content = new Content();
        content.setType(type);
        content.setDetail(detail);
        content.setPathname("");
        content.setCreatedBy(userProfile.getUserId());
        content.setAdministrators(List.of(userProfile.getUserId()));
        return contentRepository.save(content);
    }

    public Content findById(String contentId) {
        return contentRepository.findById(contentId).orElseThrow(() -> new ContentNotFoundException("Content not found"));
    }

    public Content findByPathname(String pathname) {
        return contentRepository.findByPathname(pathname).orElseThrow(() -> new ContentNotFoundException("Content with pathname '" + pathname + "' not found."));
    }

    public List<Content> findByTag(String tag) {
        return contentRepository.findByTag(tag);

    }

    // TODO there needs to be a way to get items that are not expired
    public List<Content> findByType(String type) {
        return contentRepository.findByType(type);

    }

    // TODO there needs to be a way to get items that are not expired
    public List<Content> findByTagAndType(String tag, String type) {
        return contentRepository.findByTagAndType(tag, type);

    }

    public List<Content> findByUser() {
        return contentRepository.findAll();

    }

    public List<Content> findByUserAndType() {
        return contentRepository.findAll();

    }

    // TODO there needs to be a way to get items that are not expired
    public List<Content> findAll() {
        return contentRepository.findAll();
    }

    public Content update(String contentId, ContentDetail newDetail) {
        Content content = findById(contentId);
        checkPermission(content);
        content.getDetail().update(content, newDetail);
        content.setUpdatedAt(LocalDateTime.now());
        return contentRepository.save(content);
    }

    public Content update(String contentId, UpdateType updateType, Map<String, Object> updateData) {
        Content content = findById(contentId);
        checkPermission(content);
        content.getDetail().update(updateType, updateData);
        content.setUpdatedAt(LocalDateTime.now());
        return contentRepository.save(content);
    }

    public void delete(String contentId) {
        Content content = findById(contentId);
        checkPermission(content);
        contentRepository.deleteById(contentId);
    }

    public void heart(String contentId) {
    }

    public void bookmark(String contentId) {
    }

    private void checkPermission(Content content) {
        if (userService.isAdmin()) {
            return;
        }

        String userId = userService.getUserId();
        if (!content.getAdministrators().contains(userId)) {
            throw new PermissionDeniedException("You do not have permission to access this resource");
        }
    }

    public String createUniqueSlug(String name, String contentType) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String baseSlug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        List<Content> matchingSlugs = contentRepository.findBySlugRegexAndType("^" + baseSlug + "(-\\d+)?$", contentType);

        return generateSlugFromMatches(baseSlug, matchingSlugs);
    }

    private String generateSlugFromMatches(String baseSlug, List<Content> matchingSlugs) {
        if (matchingSlugs.isEmpty()) {
            return baseSlug;
        }

        Pattern pattern = Pattern.compile(Pattern.quote(baseSlug) + "-(\\d+)$");
        int maxNumber = 0;
        boolean baseSlugExists = false;

        for (Content content : matchingSlugs) {
            String slug = content.getSlug();
            if (slug.equals(baseSlug)) {
                baseSlugExists = true;
            }

            Matcher matcher = pattern.matcher(slug);
            if (matcher.find()) {
                maxNumber = Math.max(maxNumber, Integer.parseInt(matcher.group(1)));
            }
        }

        if (baseSlugExists || maxNumber > 0) {
            return baseSlug + "-" + (maxNumber + 1);
        }

        return baseSlug;
    }
}
