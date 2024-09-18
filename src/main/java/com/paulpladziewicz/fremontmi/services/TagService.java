package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.NeighborServicesProfile;
import com.paulpladziewicz.fremontmi.models.TagUsage;
import com.paulpladziewicz.fremontmi.repositories.TagAutocompleteRepository;
import com.paulpladziewicz.fremontmi.repositories.TagRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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



    // Fetch all distinct tags by querying for documents and manually processing the tags field
    public List<String> findAllDistinctTags() {
        // Query to find all documents where "tags" exists and is not an empty array
        Query query = new Query();
        query.addCriteria(Criteria.where("tags").exists(true).not().size(0));

        // Find documents with non-empty tags
        List<NeighborServicesProfile> profiles = mongoTemplate.find(query, NeighborServicesProfile.class, "neighbor_services_profiles");

        // Use a Set to automatically handle uniqueness
        Set<String> distinctTags = new HashSet<>();

        // Loop through all profiles and collect tags
        for (NeighborServicesProfile profile : profiles) {
            if (profile.getTags() != null) {
                for (String tag : profile.getTags()) {
                    if (tag != null && !tag.trim().isEmpty()) {
                        distinctTags.add(tag);  // Add only non-empty, non-null tags
                    }
                }
            }
        }

        // Convert the set of distinct tags to a list
        return new ArrayList<>(distinctTags);
    }

    public List<TagUsage> getTopTags(int limit) {
        // Step 1: Unwind the "tags" array
        AggregationOperation unwindTags = Aggregation.unwind("tags");

        // Step 2: Group by tag and count occurrences
        GroupOperation groupByTag = Aggregation.group("tags").count().as("count");

        // Step 3: Sort by count in descending order
        AggregationOperation sortByCount = Aggregation.sort(Sort.by(Sort.Direction.DESC, "count"));

        // Step 4: Limit the results to the top 'n' tags
        AggregationOperation limitResults = Aggregation.limit(limit);

        // Step 5: Combine the aggregation steps into a pipeline
        Aggregation aggregation = Aggregation.newAggregation(unwindTags, groupByTag, sortByCount, limitResults);

        // Step 6: Execute the aggregation query using MongoTemplate
        AggregationResults<TagUsage> results = mongoTemplate.aggregate(aggregation, "neighbor_services_profiles", TagUsage.class);

        // Return the list of top tags
        return results.getMappedResults();
    }
}
