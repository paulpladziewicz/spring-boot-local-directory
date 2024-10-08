package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.exceptions.ContentNotFoundException;
import com.paulpladziewicz.fremontmi.models.*;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.text.Normalizer;

@Service
public class BusinessService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessService.class);

    private final ContentRepository contentRepository;

    private final UserService userService;

    private final StripeService stripeService;

    private final TagService tagService;

    private final EmailService emailService;

    public BusinessService(ContentRepository contentRepository, UserService userService, StripeService stripeService, TagService tagService, EmailService emailService) {
        this.contentRepository = contentRepository;
        this.userService = userService;
        this.stripeService = stripeService;
        this.tagService = tagService;
        this.emailService = emailService;
    }

    public ServiceResponse<Business> createBusiness(Business business) {
        UserProfile userProfile = userService.getUserProfile();

        business.setType(ContentTypes.BUSINESS.getContentType());
        business.setSlug(createUniqueSlug(business.getName()));
        business.setPathname("/businesses/" + business.getSlug());
        business.setVisibility(ContentVisibility.RESTRICTED.getVisibility());
        business.setStatus(ContentStatus.REQUIRES_ACTIVE_SUBSCRIPTION.getStatus());
        business.setCreatedBy(userProfile.getUserId());
        business.setAdministrators(List.of(userProfile.getUserId()));

        List<String> validatedTags = tagService.addTags(business.getTags(), ContentTypes.BUSINESS.getContentType());
        business.setTags(validatedTags);

        ServiceResponse<Map<String, Object>> createSubscriptionResponse = stripeService.createSubscription(business.getPriceId());

        if (createSubscriptionResponse.hasError()) {
            logger.error("Error when trying to create a subscription");
            return ServiceResponse.error(createSubscriptionResponse.errorCode());
        }

        Map<String, Object> stripeDetails = createSubscriptionResponse.value();

        business.setStripeDetails(stripeDetails);

        Business savedBusiness = saveBusiness(business);

        userProfile.getBusinessIds().add(savedBusiness.getId());

        userService.saveUserProfile(userProfile);

        return ServiceResponse.value(savedBusiness);
    }

    public Business saveBusiness(Business business) {
        return contentRepository.save(business);
    }

    private Boolean hasPermission(String userId, Business business) {
        return business.getAdministrators().contains(userId);
    }

    public String createUniqueSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String baseSlug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        List<Content> matchingSlugs = contentRepository.findBySlugRegexAndType("^" + baseSlug + "(-\\d+)?$", ContentTypes.BUSINESS.getContentType());

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

    public List<Business> findAllBusinesses(String tag) {
        if (tag != null && !tag.isEmpty()) {
            return contentRepository.findByTagAndType(tag, ContentTypes.BUSINESS.getContentType(), Business.class);
        } else {
            return contentRepository.findAllByType(ContentTypes.BUSINESS.getContentType(), Business.class);
        }
    }

    public Business findBusinessById(String id) {
        return contentRepository.findById(id, Business.class)
                .orElseThrow(() -> new ContentNotFoundException("Business with id '" + id + "' not found."));

    }

    public Business findBusinessBySlug(String slug) {
        return contentRepository.findBySlugAndType(slug, ContentTypes.BUSINESS.getContentType(), Business.class)
                .orElseThrow(() -> new ContentNotFoundException("Business with slug '" + slug + "' not found."));
    }

    public List<Business> findBusinessesByUser () {
        UserProfile userProfile = userService.getUserProfile();

        List<Content> contents = contentRepository.findAllById(userProfile.getBusinessIds());

        return contents.stream()
                .filter(content -> content instanceof Business)
                .map(content -> (Business) content)
                .collect(Collectors.toList());
    }

    @Transactional
    public Business updateBusiness(Business updatedBusiness) {
        String userId = userService.getUserId();

        Business existingBusiness = findBusinessById(updatedBusiness.getId());

        // TODO checkPermissions

        List<String> oldTags = existingBusiness.getTags();
        List<String> newTags = updatedBusiness.getTags();

        if (newTags != null) {
            tagService.updateTags(newTags, oldTags != null ? oldTags : new ArrayList<>(), ContentTypes.BUSINESS.getContentType());
        }

        if (!existingBusiness.getName().equals(updatedBusiness.getName())) {
            String newSlug = createUniqueSlug(updatedBusiness.getName());
            existingBusiness.setSlug(newSlug);
            existingBusiness.setPathname("/businesses/" + newSlug);
        }

        updateBusinessProperties(existingBusiness, updatedBusiness);

        existingBusiness.setUpdatedBy(userId);
        existingBusiness.setUpdatedAt(LocalDateTime.now());

        return saveBusiness(existingBusiness);
    }



    public ServiceResponse<Boolean> deleteBusiness(String businessId) {
        UserProfile userProfile = userService.getUserProfile();
        Business business = findBusinessById(businessId);

        // TODO checkPermissions

        if (business.getSubscriptionId() != null && !business.getSubscriptionId().isEmpty()) {
            ServiceResponse<Boolean> cancelSubscriptionResponse = stripeService.cancelSubscriptionAtPeriodEnd(business.getSubscriptionId());

            if (cancelSubscriptionResponse.hasError()) {
                return ServiceResponse.error(cancelSubscriptionResponse.errorCode());
            }
        }

        tagService.removeTags(business.getTags(), ContentTypes.BUSINESS.getContentType());

        List<String> businessIds = userProfile.getBusinessIds();
        businessIds.remove(business.getId());
        userProfile.setBusinessIds(businessIds);
        userService.saveUserProfile(userProfile);

        contentRepository.deleteById(businessId);
        return ServiceResponse.value(true);
    }

    private void updateBusinessProperties(Business existingBusiness, Business updatedBusiness) {
        existingBusiness.setName(updatedBusiness.getName());
        existingBusiness.setHeadline(updatedBusiness.getHeadline());
        existingBusiness.setDescription(updatedBusiness.getDescription());

        if (updatedBusiness.getTags() != null && !updatedBusiness.getTags().isEmpty()) {
            existingBusiness.setTags(updatedBusiness.getTags());
        }

        //existingBusiness.setAdministrators(updatedBusiness.getAdministrators());
        existingBusiness.setAddress(updatedBusiness.getAddress());
        existingBusiness.setPhoneNumber(updatedBusiness.getPhoneNumber());
        existingBusiness.setEmail(updatedBusiness.getEmail());
        existingBusiness.setWebsite(updatedBusiness.getWebsite());
        existingBusiness.setDisplayEmail(updatedBusiness.isDisplayEmail());
    }

    public ServiceResponse<Boolean> handleContactFormSubmission(String slug, String name, String email, String message) {
        Business business = findBusinessBySlug(slug);

        try {
             emailService.sendContactBusinessEmail(business.getEmail(), name, email, message);

            return ServiceResponse.value(true);
        } catch (Exception e) {
            logger.error("Error when trying to send contact form submission email", e);
            return ServiceResponse.error("unexpected_error");
        }
    }
}
