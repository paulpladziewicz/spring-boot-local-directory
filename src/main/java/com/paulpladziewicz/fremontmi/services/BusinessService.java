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
import java.util.ArrayList;
import java.util.List;

@Service
public class BusinessService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessService.class);
    private final ContentRepository contentRepository;
    private final UserService userService;
    private final SlugService slugService;
    private final TagService tagService;
    private final EmailService emailService;

    public BusinessService(ContentRepository contentRepository, UserService userService, SlugService slugService, TagService tagService, EmailService emailService) {
        this.contentRepository = contentRepository;
        this.userService = userService;
        this.slugService = slugService;
        this.tagService = tagService;
        this.emailService = emailService;
    }

    public Business create(Business business) {
        UserProfile userProfile = userService.getUserProfile();

        business.setType(ContentType.BUSINESS.getContentType());
        business.setSlug(slugService.createUniqueSlug(business.getName(), ContentType.BUSINESS.getContentType()));
        business.setPathname("/businesses/" + business.getSlug());
        business.setVisibility(ContentVisibility.PUBLIC.getVisibility());
        business.setStatus(ContentStatus.ACTIVE.getStatus());
        business.setCreatedBy(userProfile.getUserId());
        business.setAdministrators(List.of(userProfile.getUserId()));

        List<String> validatedTags = tagService.addTags(business.getTags(), ContentType.BUSINESS.getContentType());
        business.setTags(validatedTags);

        Business savedBusiness = contentRepository.save(business);

        userProfile.getBusinessIds().add(savedBusiness.getId());

        userService.saveUserProfile(userProfile);

        return savedBusiness;
    }

    public List<Business> findAll(String tag) {
        if (tag != null && !tag.isEmpty()) {
            return contentRepository.findByTagAndType(tag, ContentType.BUSINESS.getContentType(), Business.class);
        } else {
            return contentRepository.findAllByType(ContentType.BUSINESS.getContentType(), Business.class);
        }
    }


    public Business findBySlug(String slug) {
        return contentRepository.findBySlugAndType(slug, ContentType.BUSINESS.getContentType(), Business.class)
                .orElseThrow(() -> new ContentNotFoundException("Business with slug '" + slug + "' not found."));
    }

    public List<Business> findAllByUser() {
        UserProfile userProfile = userService.getUserProfile();

        return contentRepository.findAllById(userProfile.getBusinessIds(), Business.class);
    }

    @Transactional
    public Business update(Business updatedBusiness) {
        String userId = userService.getUserId();

        Business existingBusiness = findById(updatedBusiness.getId());

        checkPermission(userId, existingBusiness);

        List<String> oldTags = existingBusiness.getTags();
        List<String> newTags = updatedBusiness.getTags();

        if (newTags != null) {
            tagService.updateTags(newTags, oldTags != null ? oldTags : new ArrayList<>(), ContentType.BUSINESS.getContentType());
        }

        if (!existingBusiness.getName().equals(updatedBusiness.getName())) {
            String newSlug = slugService.createUniqueSlug(updatedBusiness.getName(), ContentType.BUSINESS.getContentType());
            existingBusiness.setSlug(newSlug);
            existingBusiness.setPathname("/businesses/" + newSlug);
        }

        updateBusinessProperties(existingBusiness, updatedBusiness);

        existingBusiness.setUpdatedBy(userId);
        existingBusiness.setUpdatedAt(LocalDateTime.now());

        return contentRepository.save(existingBusiness);
    }

    public void delete(String businessId) {
        UserProfile userProfile = userService.getUserProfile();
        Business business = findById(businessId);

        checkPermission(userProfile.getUserId(), business);

        tagService.removeTags(business.getTags(), ContentType.BUSINESS.getContentType());

        List<String> businessIds = userProfile.getBusinessIds();
        businessIds.remove(business.getId());
        userProfile.setBusinessIds(businessIds);
        userService.saveUserProfile(userProfile);

        contentRepository.deleteById(businessId);
    }

    private void updateBusinessProperties(Business existingBusiness, Business updatedBusiness) {
        existingBusiness.setName(updatedBusiness.getName());
        existingBusiness.setHeadline(updatedBusiness.getHeadline());
        existingBusiness.setDescription(updatedBusiness.getDescription());

        if (updatedBusiness.getTags() != null && !updatedBusiness.getTags().isEmpty()) {
            existingBusiness.setTags(updatedBusiness.getTags());
        }

        existingBusiness.setAddress(updatedBusiness.getAddress());
        existingBusiness.setPhoneNumber(updatedBusiness.getPhoneNumber());
        existingBusiness.setEmail(updatedBusiness.getEmail());
        existingBusiness.setWebsite(updatedBusiness.getWebsite());
        existingBusiness.setDisplayEmail(updatedBusiness.isDisplayEmail());
    }

    public Boolean handleContactFormSubmission(String slug, String name, String email, String message) {
        Business business = findBySlug(slug);

        try {
             emailService.sendContactBusinessEmail(business.getEmail(), name, email, message);

            return true;
        } catch (Exception e) {
            logger.error("Error when trying to send contact form submission email", e);
            return false;
        }
    }

    private void checkPermission(String userId, Business business) {
        if (!business.getAdministrators().contains(userId)) {
            throw new PermissionDeniedException("You do not have permission to access this resource");
        }
    }
}
