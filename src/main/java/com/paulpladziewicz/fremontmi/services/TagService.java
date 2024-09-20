package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Content;
import com.paulpladziewicz.fremontmi.models.NeighborServicesProfile;
import com.paulpladziewicz.fremontmi.models.TagUsage;
import com.paulpladziewicz.fremontmi.repositories.TagAutocompleteRepository;
import com.paulpladziewicz.fremontmi.repositories.TagRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TagService {

    private final TagRepository tagRepository;

    private final TagAutocompleteRepository tagAutocompleteRepository;

    private final MongoTemplate mongoTemplate;

    public TagService(TagRepository tagRepository, TagAutocompleteRepository tagAutocompleteRepository, MongoTemplate mongoTemplate) {
        this.tagRepository = tagRepository;
        this.tagAutocompleteRepository = tagAutocompleteRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<String> autocompleteList(String text) {
        return tagAutocompleteRepository.searchTagsByName(text);
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

    public List<TagUsage> getTagUsageFromContent(List<Content> contentList, int max) {
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

}
