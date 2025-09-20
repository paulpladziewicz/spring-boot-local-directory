package com.paulpladziewicz.fremontmi.discovery;

import com.paulpladziewicz.fremontmi.content.Content;
import com.paulpladziewicz.fremontmi.content.ContentType;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TagService {

    private final TagRepository tagRepository;

    private final MongoTemplate mongoTemplate;

    public TagService(TagRepository tagRepository, MongoTemplate mongoTemplate) {
        this.tagRepository = tagRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<TagUsage> getTopTags(int limit) {
        // Step 1: Group by tag name and count total occurrences
        GroupOperation groupByTag = Aggregation.group("name")
                .sum("count").as("count");

        // Step 2: Sort by count in descending order
        AggregationOperation sortByCount = Aggregation.sort(Sort.by(Sort.Direction.DESC, "count"));

        // Step 3: Limit the results to the top 'n' tags
        AggregationOperation limitResults = Aggregation.limit(limit);

        // Step 4: Combine the aggregation steps into a pipeline
        Aggregation aggregation = Aggregation.newAggregation(groupByTag, sortByCount, limitResults);

        // Step 5: Execute the aggregation query using MongoTemplate
        AggregationResults<TagUsage> results = mongoTemplate.aggregate(aggregation, "tags", TagUsage.class);

        // Return the list of top tags
        return results.getMappedResults();
    }

    @Cacheable(value = "globalPopularTags", unless = "#result == null || #result.isEmpty()")
    public List<TagUsage> getGlobalPopularTags(int limit) {
        return getTopTags(limit);
    }

    // Cache popular tags by content type
    @Cacheable(value = "popularTags", key = "#contentType", unless = "#result == null || #result.isEmpty()")
    public List<TagUsage> getPopularTagsByContentType(String contentType, int limit) {
        // Step 1: Project the count for the specific content type
        ProjectionOperation projectContentTypeCount = Aggregation.project("name", "displayName")
                .andExpression("countByContentType." + contentType).as("count");

        // Step 2: Match only tags where the count for the content type exists and is greater than 0
        MatchOperation matchNonZeroCount = Aggregation.match(Criteria.where("count").gt(0));

        // Step 3: Sort by the content type-specific count in descending order
        AggregationOperation sortByCount = Aggregation.sort(Sort.by(Sort.Direction.DESC, "count"));

        // Step 4: Limit the results to the top 'n' tags
        AggregationOperation limitResults = Aggregation.limit(limit);

        // Step 5: Combine the aggregation steps into a pipeline
        Aggregation aggregation = Aggregation.newAggregation(projectContentTypeCount, matchNonZeroCount, sortByCount, limitResults);

        // Step 6: Execute the aggregation query using MongoTemplate
        AggregationResults<TagUsage> results = mongoTemplate.aggregate(aggregation, "tags", TagUsage.class);

        // Return the list of top tags for the specific content type
        return results.getMappedResults();
    }

    public List<TagUsage> getTagUsageFromContent(Page<Content> contentList, int max) {
        Map<String, Integer> tagCountMap = new HashMap<>();

        for (Content content : contentList) {
            List<String> tags = content.getTags();
            if (tags != null) {
                for (String tag : tags) {
                    tagCountMap.put(tag, tagCountMap.getOrDefault(tag, 0) + 1);
                }
            }
        }

        return tagCountMap.entrySet().stream()
                .map(entry -> new TagUsage(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(TagUsage::getCount).reversed())
                .limit(max)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<String> addTags(List<String> displayNames, ContentType contentType) {
        List<String> validatedDisplayNames = new ArrayList<>();
        Set<String> canonicalTagsSet = new HashSet<>();  // Track canonical versions of tags

        for (String displayName : displayNames) {
            // Generate the canonical name (lowercase, no spaces, but keep special characters)
            String canonicalName = generateCanonicalName(displayName);

            // Skip if this canonical name has already been processed
            if (canonicalTagsSet.contains(canonicalName)) {
                continue;
            }
            canonicalTagsSet.add(canonicalName);

            // Format the display name
            String formattedDisplayName = formatDisplayName(displayName);

            // Check if the tag exists in the database
            Optional<Tag> optionalTag = tagRepository.findByName(canonicalName);

            if (optionalTag.isPresent()) {
                Tag existingTag = optionalTag.get();
                validatedDisplayNames.add(existingTag.getDisplayName());  // Use existing display name from DB
                existingTag.incrementCountForContentType(contentType);
                existingTag.setCount(existingTag.getCount() + 1);
                tagRepository.save(existingTag);
            } else {
                // Tag does not exist, create a new one
                Tag newTag = new Tag(canonicalName, formattedDisplayName);
                validatedDisplayNames.add(formattedDisplayName);  // Use the formatted display name
                newTag.incrementCountForContentType(contentType);
                newTag.setCount(1);
                tagRepository.save(newTag);
            }
        }

        return validatedDisplayNames;  // Return the validated display names
    }

    @Transactional
    public List<String> updateTags(List<String> newDisplayNames, List<String> oldDisplayNames, ContentType contentType) {
        Set<String> newCanonicalTags = newDisplayNames.stream()
                .map(this::generateCanonicalName)
                .collect(Collectors.toSet());

        Set<String> oldCanonicalTags = oldDisplayNames.stream()
                .map(this::generateCanonicalName)
                .collect(Collectors.toSet());

        Set<String> tagsToAdd = new HashSet<>(newCanonicalTags);
        tagsToAdd.removeAll(oldCanonicalTags);

        Set<String> tagsToRemove = new HashSet<>(oldCanonicalTags);
        tagsToRemove.removeAll(newCanonicalTags);

        List<String> validatedDisplayNames = new ArrayList<>();

        for (String displayName : newDisplayNames) {
            String canonicalName = generateCanonicalName(displayName);

            if (tagsToAdd.contains(canonicalName)) {
                validatedDisplayNames.addAll(addTags(List.of(displayName), contentType));
            } else if (!tagsToRemove.contains(canonicalName)) {
                Optional<Tag> existingTag = tagRepository.findByName(canonicalName);
                validatedDisplayNames.add(existingTag.map(Tag::getDisplayName).orElse(formatDisplayName(displayName)));
            }
        }

        if (!tagsToRemove.isEmpty()) {
            List<String> tagsToRemoveDisplayNames = oldDisplayNames.stream()
                    .filter(tag -> tagsToRemove.contains(generateCanonicalName(tag)))
                    .collect(Collectors.toList());
            removeTags(tagsToRemoveDisplayNames, contentType);
        }

        return validatedDisplayNames;
    }

    @Transactional
    public void removeTags(List<String> displayNames, ContentType contentType) {
        Set<String> processedCanonicalTags = new HashSet<>();  // Track processed canonical versions of tags

        for (String displayName : displayNames) {
            // Generate the canonical name (lowercase, no spaces, but keep special characters)
            String canonicalName = generateCanonicalName(displayName);

            // Skip if this canonical name has already been processed
            if (processedCanonicalTags.contains(canonicalName)) {
                continue;
            }
            processedCanonicalTags.add(canonicalName);

            // Check if the tag exists in the database
            Optional<Tag> optionalTag = tagRepository.findByName(canonicalName);

            if (optionalTag.isPresent()) {
                Tag existingTag = optionalTag.get();

                // Decrement the overall count
                int updatedCount = existingTag.getCount() - 1;
                existingTag.setCount(Math.max(0, updatedCount));  // Ensure count doesn't go below 0

                // Decrement the count for the specific content type
                existingTag.decrementCountForContentType(contentType);
                // Save changes if counts are updated
                tagRepository.save(existingTag);
            }
        }
    }

    // Helper method to generate the canonical form of a tag
    private String generateCanonicalName(String displayName) {
        // Remove all characters except lowercase letters, spaces, and hyphens
        return displayName.toLowerCase().replaceAll("[^a-z\\s-]", "").replaceAll("\\s+", "");
    }

    // Helper method to format the display name of a tag
    private String formatDisplayName(String name) {
        // Remove invalid characters and capitalize the words
        String cleanedName = name.replaceAll("[^a-zA-Z\\s-]", "").trim();

        return Arrays.stream(cleanedName.split("-"))
                .map(part -> Arrays.stream(part.split("\\s+"))
                        .map(word -> word.equals(word.toUpperCase()) ? word : capitalizeFirstLetter(word))
                        .collect(Collectors.joining(" "))
                ).collect(Collectors.joining("-"));
    }

    // Helper to capitalize the first letter of each word
    private String capitalizeFirstLetter(String word) {
        return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }
}
