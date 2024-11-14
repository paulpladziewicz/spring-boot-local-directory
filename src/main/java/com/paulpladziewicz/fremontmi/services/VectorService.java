package com.paulpladziewicz.fremontmi.services;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.search.FieldSearchPath;
import com.paulpladziewicz.fremontmi.models.Content;
import com.paulpladziewicz.fremontmi.models.ContentVector;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import com.paulpladziewicz.fremontmi.repositories.ContentVectorRepository;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.vectorSearch;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.search.SearchPath.fieldPath;

import static java.util.Arrays.asList;

@Service
public class VectorService {

    private final ContentService contentService;
    @Value("${openai.api.key}")
    private String apiKey;

    private final ContentRepository contentRepository;
    private final ContentVectorRepository contentVectorRepository;
    private final RestTemplate restTemplate;
    private final MongoClient mongoClient;

    public VectorService(ContentRepository contentRepository, ContentVectorRepository contentVectorRepository, RestTemplate restTemplate, MongoClient mongoClient, ContentService contentService) {
        this.contentRepository = contentRepository;
        this.contentVectorRepository = contentVectorRepository;
        this.restTemplate = restTemplate;
        this.mongoClient = mongoClient;
        this.contentService = contentService;
    }

    public List<Content> searchRelevantContent(String prompt) {
        MongoDatabase database = mongoClient.getDatabase("fremontmi");
        MongoCollection<Document> collection = database.getCollection("content_vectors");
        List<Double> queryVector = generateVectorForPrompt(prompt);

        String indexName = "vector_index";
        FieldSearchPath fieldSearchPath = fieldPath("vector");
        int limit = 5;
        int numCandidates = 5;
        double relevanceThreshold = 0.67;

        List<Bson> pipeline = asList(
                vectorSearch(
                        fieldSearchPath,
                        queryVector,
                        indexName,
                        limit,
                        numCandidates),
                project(
                        fields(include("_id"), metaVectorSearchScore("score"))
                )
        );

        List<String> results = collection.aggregate(pipeline)
                .map(doc -> {
                    System.out.println(doc.toJson());
                    double score = doc.getDouble("score");
                    if (score > relevanceThreshold) {
                        return doc.getObjectId("_id").toString();
                    }
                    return null;
                })
                .into(new ArrayList<>())  // Collect all documents into a list
                .stream()
                .filter(Objects::nonNull)  // Remove null values for irrelevant items
                .collect(Collectors.toList());

        return contentService.findByArrayOfIds(results);
    }

    private List<Double> generateVectorForPrompt(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
                "model", "text-embedding-ada-002",
                "input", prompt
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response;

        try {
            response = restTemplate.postForEntity("https://api.openai.com/v1/embeddings", request, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch vector from OpenAI: " + e.getMessage(), e);
        }

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Invalid response from OpenAI: " + response.getStatusCode());
        }

        return (List<Double>) ((Map<String, Object>) ((List<?>) response.getBody().get("data")).get(0)).get("embedding");
    }

    public List<Double> generateVector(String contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found"));

        String inputText = content.getDetail().getTitle() + " " + content.getDetail().getDescription(); // Combine title and description

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
                "model", "text-embedding-ada-002",
                "input", inputText
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("https://api.openai.com/v1/embeddings", request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            List<Double> vector = (List<Double>) ((Map<String, Object>) ((List<?>) response.getBody().get("data")).get(0)).get("embedding");

            ContentVector contentVector = new ContentVector();
            contentVector.setContentId(contentId);
            contentVector.setVector(vector);

            contentVectorRepository.save(contentVector);
            return vector;
        } else {
            throw new RuntimeException("Failed to generate vector: " + response.getStatusCode());
        }
    }
}

