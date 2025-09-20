package com.paulpladziewicz.fremontmi.discovery;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "search_history")
public class SearchHistory {
    @Id
    private String id;
    private String prompt;
    private List<ResultWithScore> allResultsWithScores;
    private LocalDateTime timestamp;
}