package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@Document(collection = "search_history")
public class SearchHistory {
    @Id
    private String id;
    private String prompt;
    private Map<String, Double> resultsWithScores;
    private List<Content> returnedContent;
    private long timestamp;
}