package com.paulpladziewicz.fremontmi.repositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

@Repository
public class TagAutocompleteRepository {

    private final String DATABASE_NAME = "fremontmi";

    private final String COLLECTION_NAME = "tags";

    private final String CONNECTION_STRING = "mongodb+srv://ppladziewicz:Life%40312@westmichigansoftware.52fvz.mongodb.net/?retryWrites=true&w=majority&appName=WestMichiganSoftware";

    public List<String> searchTagsByName(String queryText) {
        try (MongoClient mongoClient = MongoClients.create(CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

            // Define the pipeline for the autocomplete query
            Document searchQuery = new Document("$search",
                    new Document("index", "tag_autocomplete_index")
                            .append("autocomplete",
                                    new Document("query", queryText)
                                            .append("path", "name")
                                            .append("fuzzy",
                                                    new Document("maxEdits", 1)
                                                            .append("prefixLength", 2)
                                                            .append("maxExpansions", 50))
                                            .append("tokenOrder", "sequential")));

            // Run the pipeline
            List<Document> results = collection.aggregate(Arrays.asList(
                            searchQuery,
                            limit(10), // Limit the number of results
                            project(fields(excludeId(), include("name")))))
                    .into(new ArrayList<>());

            // Convert the results into a list of strings (tag names)
            return results.stream()
                    .map(doc -> doc.getString("name"))
                    .collect(Collectors.toList());
        }
    }
}
