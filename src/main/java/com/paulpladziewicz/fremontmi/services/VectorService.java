package com.paulpladziewicz.fremontmi.services;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.search.FieldSearchPath;
import com.paulpladziewicz.fremontmi.models.Content;
import com.paulpladziewicz.fremontmi.models.ContentVector;
import com.paulpladziewicz.fremontmi.models.SearchHistory;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import com.paulpladziewicz.fremontmi.repositories.ContentVectorRepository;
import com.paulpladziewicz.fremontmi.repositories.SearchHistoryRepository;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
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
    private final SearchHistoryRepository searchHistoryRepository;
    private final RestTemplate restTemplate;
    private final MongoClient mongoClient;

    public VectorService(ContentRepository contentRepository, ContentVectorRepository contentVectorRepository, SearchHistoryRepository searchHistoryRepository, RestTemplate restTemplate, MongoClient mongoClient, ContentService contentService) {
        this.contentRepository = contentRepository;
        this.contentVectorRepository = contentVectorRepository;
        this.searchHistoryRepository = searchHistoryRepository;
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
        int numCandidates = 5;
        int limit = 5;
        double relevanceThreshold = 0.67;

        List<Bson> pipeline = asList(
                vectorSearch(fieldSearchPath, queryVector, indexName, limit, numCandidates),
                project(fields(include("_id"), metaVectorSearchScore("score")))
        );

        Map<String, Double> allResultsWithScores = collection.aggregate(pipeline)
                .map(doc -> Map.entry(doc.getObjectId("_id").toString(), doc.getDouble("score")))
                .into(new ArrayList<>())
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

        List<String> filteredResults = allResultsWithScores.entrySet().stream()
                .filter(entry -> entry.getValue() > relevanceThreshold)
                .map(Map.Entry::getKey)
                .toList();

        SearchHistory searchHistory = new SearchHistory();
        searchHistory.setPrompt(prompt);
        searchHistory.setAllResultsWithScores(allResultsWithScores);
        searchHistory.setTimestamp(LocalDateTime.now(ZoneId.of("America/Detroit")));

        searchHistoryRepository.save(searchHistory);

        return contentService.findByArrayOfIds(filteredResults);
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

    public void generateVectorsForAllContent() {
        List<Content> allContent = contentRepository.findAll();

        allContent.forEach(content -> {
            try {
                generateVector(content.getId());
            } catch (Exception e) {
                System.err.println("Failed to generate vector for content ID: " + content.getId() + ". Error: " + e.getMessage());
            }
        });
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

