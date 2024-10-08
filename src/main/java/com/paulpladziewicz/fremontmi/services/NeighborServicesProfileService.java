package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.exceptions.ContentNotFoundException;
import com.paulpladziewicz.fremontmi.exceptions.PermissionDeniedException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NeighborServicesProfileService {
    private static final Logger logger = LoggerFactory.getLogger(NeighborServicesProfileService.class);

    private final ContentRepository contentRepository;

    private final UserService userService;

    private final StripeService stripeService;

    private final TagService tagService;

    private final EmailService emailService;

    public NeighborServicesProfileService(ContentRepository contentRepository, UserService userService, StripeService stripeService, TagService tagService, EmailService emailService) {
        this.contentRepository = contentRepository;
        this.userService = userService;
        this.stripeService = stripeService;
        this.tagService = tagService;
        this.emailService = emailService;
    }

    public NeighborServicesProfile createNeighborServiceProfile(NeighborServicesProfile neighborServicesProfile) {
        UserProfile userProfile = userService.getUserProfile();

        neighborServicesProfile.setType(ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());
        neighborServicesProfile.setSlug(createUniqueSlug(neighborServicesProfile.getName()));
        neighborServicesProfile.setPathname("/neighbor-services/" + neighborServicesProfile.getSlug());
        neighborServicesProfile.setVisibility(ContentVisibility.RESTRICTED.getVisibility());
        neighborServicesProfile.setStatus(ContentStatus.REQUIRES_ACTIVE_SUBSCRIPTION.getStatus());
        neighborServicesProfile.setCreatedBy(userProfile.getUserId());

        List<String> validatedTags = tagService.addTags(neighborServicesProfile.getTags(), ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());
        neighborServicesProfile.setTags(validatedTags);

        Map<String, Object> stripeDetails  = stripeService.createSubscription(neighborServicesProfile.getPriceId());

        neighborServicesProfile.setStripeDetails(stripeDetails);

        return contentRepository.save(neighborServicesProfile);
    }

    public List<NeighborServicesProfile> findAllActiveNeighborServices(String tag) {
        List<Content> contents;

        if (tag != null && !tag.isEmpty()) {
            contents = contentRepository.findByTagAndType(tag, ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());
        } else {
            contents = contentRepository.findAllByType(ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());
        }

        return contents.stream()
                .filter(content -> content instanceof NeighborServicesProfile) // Ensure type safety
                .map(content -> (NeighborServicesProfile) content)
                .collect(Collectors.toList());
    }

    public NeighborServicesProfile findNeighborServiceProfileById(String id) {
            return contentRepository.findById(id, NeighborServicesProfile.class)
                    .orElseThrow(() -> new ContentNotFoundException("Business with id '" + id + "' not found."));
    }

    public NeighborServicesProfile findNeighborServiceProfileBySlug(String slug) {
            return contentRepository.findBySlugAndType(slug, ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType(), NeighborServicesProfile.class)
                    .orElseThrow(() -> new ContentNotFoundException("Business with slug '" + slug + "' not found."));
    }

    public Optional<NeighborServicesProfile> findNeighborServiceProfileByUserId() {
        String userId = userService.getUserId();

        Optional<Content> optionalContent = contentRepository.findByCreatedBy(userId);

        return optionalContent
                .filter(content -> content instanceof NeighborServicesProfile)
                .map(content -> (NeighborServicesProfile) content);
    }

    @Transactional
    public NeighborServicesProfile updateNeighborServiceProfile(NeighborServicesProfile neighborServiceProfile) {
        String userId = userService.getUserId();

        NeighborServicesProfile existingProfile = findNeighborServiceProfileById(neighborServiceProfile.getId());

        checkPermission(userId, existingProfile);

        List<String> oldTags = existingProfile.getTags();
        List<String> newTags = neighborServiceProfile.getTags();

        if (newTags != null) {
            tagService.updateTags(newTags, oldTags != null ? oldTags : new ArrayList<>(), ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());
        }

        if (!existingProfile.getName().equals(neighborServiceProfile.getName())) {
            String newSlug = createUniqueSlug(neighborServiceProfile.getName());
            existingProfile.setSlug(newSlug);
            existingProfile.setPathname("/neighbor-services/" + newSlug);
        }

        updateExistingNeighborServiceProfile(existingProfile, neighborServiceProfile);

        return contentRepository.save(existingProfile);
    }


    @Transactional
    public void deleteNeighborService(String neighborServiceId) {
        String userId = userService.getUserId();

        NeighborServicesProfile neighborServicesProfile = findNeighborServiceProfileById(neighborServiceId);

        checkPermission(userId, neighborServicesProfile);

        if (neighborServicesProfile.getSubscriptionId() != null && !neighborServicesProfile.getSubscriptionId().isEmpty()) {
            stripeService.cancelSubscriptionAtPeriodEnd(neighborServicesProfile.getSubscriptionId());
        }

        tagService.removeTags(neighborServicesProfile.getTags(), ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());

        contentRepository.deleteById(neighborServiceId);
    }

    private void checkPermission(String userId, NeighborServicesProfile neighborServicesProfile) {
        if (!neighborServicesProfile.getCreatedBy().equals(userId)) {
            throw new PermissionDeniedException("User does not have permission to modify this event.");
        }
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

    public void handleContactFormSubmission(String slug, String name, String email, String message) {
        NeighborServicesProfile neighborServicesProfile = findNeighborServiceProfileBySlug(slug);

        emailService.sendContactNeighborServiceProfileEmail(neighborServicesProfile.getEmail(), name, email, message);
    }

    public String createUniqueSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String baseSlug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        List<Content> matchingSlugs = contentRepository.findBySlugRegexAndType("^" + baseSlug + "(-\\d+)?$", ContentTypes.NEIGHBOR_SERVICES_PROFILE.getContentType());

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
                int number = Integer.parseInt(matcher.group(1));
                maxNumber = Math.max(maxNumber, number);
            }
        }

        if (baseSlugExists) {
            return baseSlug + "-" + (maxNumber + 1);
        } else if (maxNumber > 0) {
            return baseSlug + "-" + (maxNumber + 1);
        } else {
            return baseSlug;
        }
    }
}
