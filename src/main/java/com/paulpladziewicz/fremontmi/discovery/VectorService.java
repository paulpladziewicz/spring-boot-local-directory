package com.paulpladziewicz.fremontmi.discovery;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.search.FieldSearchPath;
import com.paulpladziewicz.fremontmi.content.Business;
import com.paulpladziewicz.fremontmi.content.Content;
import com.paulpladziewicz.fremontmi.content.ContentDetail;
import com.paulpladziewicz.fremontmi.content.ContentRepository;
import com.paulpladziewicz.fremontmi.content.ContentService;
import com.paulpladziewicz.fremontmi.content.ContentType;
import com.paulpladziewicz.fremontmi.content.ContentVector;
import com.paulpladziewicz.fremontmi.content.ContentVectorRepository;
import com.paulpladziewicz.fremontmi.content.Event;
import com.paulpladziewicz.fremontmi.content.Group;
import com.paulpladziewicz.fremontmi.content.NeighborServicesProfile;

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

        String indexName = "vector_index_cosine";
        FieldSearchPath fieldSearchPath = fieldPath("vector");
        int numCandidates = 5;
        int limit = 5;
        double relevanceThreshold = 0.65;

        List<Bson> pipeline = asList(
                vectorSearch(fieldSearchPath, queryVector, indexName, limit, numCandidates),
                project(fields(include("_id"), metaVectorSearchScore("score")))
        );

        List<ResultWithScore> allResultsWithScores = collection.aggregate(pipeline)
                .map(doc -> new ResultWithScore(doc.getObjectId("_id").toString(), doc.getDouble("score")))
                .into(new ArrayList<>());

        System.out.println(allResultsWithScores);

        List<String> filteredResults = allResultsWithScores.stream()
                .filter(result -> result.getScore() > relevanceThreshold)
                .map(ResultWithScore::getId)
                .toList();

        System.out.println(filteredResults);

        SearchHistory searchHistory = new SearchHistory();
        searchHistory.setPrompt(prompt);
        searchHistory.setAllResultsWithScores(allResultsWithScores);
        searchHistory.setTimestamp(LocalDateTime.now(ZoneId.of("America/Detroit")));

        searchHistoryRepository.save(searchHistory);

        List<Content> unorderedContent = contentService.findByArrayOfIds(filteredResults);
        Map<String, Content> contentMap = unorderedContent.stream()
                .collect(Collectors.toMap(Content::getId, content -> content));

        return filteredResults.stream()
                .map(contentMap::get)
                .toList();
    }

    private List<Double> generateVectorForPrompt(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
                "model", "text-embedding-3-large",
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

        String inputText = buildInputText(content);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
                "model", "text-embedding-3-large",
                "input", inputText
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("https://api.openai.com/v1/embeddings", request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            List<Double> vector = (List<Double>) ((Map<String, Object>) ((List<?>) response.getBody().get("data")).get(0)).get("embedding");

            ContentVector contentVector = new ContentVector();
            contentVector.setContentId(contentId);
            contentVector.setInputText(inputText);
            contentVector.setVector(vector);

            contentVectorRepository.save(contentVector);
            return vector;
        } else {
            throw new RuntimeException("Failed to generate vector: " + response.getStatusCode());
        }
    }

    private String buildInputText(Content content) {
        ContentDetail detail = content.getDetail();
        ContentType type = content.getType();

        StringBuilder inputBuilder = new StringBuilder("Type: ").append(type.toHyphenatedString());

        if (detail instanceof Group group) {
            inputBuilder.append(" | Title: ").append(group.getTitle())
                    .append(" | Description: ").append(group.getDescription());
        } else if (detail instanceof Event event) {
            inputBuilder.append(" | Title: ").append(event.getTitle())
                    .append(" | Description: ").append(event.getDescription());
        } else if (detail instanceof Business business) {
            inputBuilder.append(" | Title: ").append(business.getTitle())
                    .append(" | Description: ").append(business.getDescription());
        } else if (detail instanceof NeighborServicesProfile profile) {
            inputBuilder.append(" | Title: ").append(profile.getTitle()).append(" | Description: ").append(profile.getDescription());
        }

        if (!content.getTags().isEmpty()) {
            inputBuilder.append(" | Tags: ").append(String.join(", ", content.getTags()));
        }

        return inputBuilder.toString();
    }

    public void delete(String contentId) {
        contentVectorRepository.deleteById(contentId);
    }
}

