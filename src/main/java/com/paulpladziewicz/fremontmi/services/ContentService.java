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

    public Content create(ContentType type, ContentDetail detail) {
        UserProfile userProfile = userService.getUserProfile();
        Content content = new Content();
        content.setType(type);
        content.setDetail(detail);
        content.setPathname("");
        content.setCreatedBy(userProfile.getUserId());
        content.setAdministrators(List.of(userProfile.getUserId()));
        content.setCreatedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());
        return contentRepository.save(content);
    }

    public Content findById(String contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Content not found"));
    }

    public Content update(String contentId, ContentDetail updatedDetail) {
        Content content = findById(contentId);
        checkPermission(content);

        switch (content.getType()) {
            case BUSINESS:
                if (updatedDetail instanceof Business) {
                    Business businessDetail = (Business) content.getDetail();
                    businessDetail.update(content, updatedDetail);
                } else {
                    throw new IllegalArgumentException("ContentDetail type mismatch for BUSINESS");
                }
                break;
            case EVENT:
                if (updatedDetail instanceof Event) {
                    Event eventDetail = (Event) content.getDetail();
                    eventDetail.update(content, updatedDetail);
                } else {
                    throw new IllegalArgumentException("ContentDetail type mismatch for EVENT");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported content type: " + content.getType());
        }

        updateMetadata(content, updatedDetail);
        content.setUpdatedBy(userService.getUserId());
        content.setUpdatedAt(LocalDateTime.now());
        return contentRepository.save(content);
    }

    public void delete(String contentId) {
        Content content = findById(contentId);
        checkPermission(content);
        tagService.removeTags(content.getDetail().getTags(), content.getType());
        contentRepository.deleteById(contentId);
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

    public void updateMetadata(Content content, ContentDetail updatedDetail) {
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

        basePathname = "/" + contentType.getContentType() + "/" + basePathname;

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
}


