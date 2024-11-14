package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

@Data
public class ResultWithScore {
    private String id;
    private Double score;

    public ResultWithScore(String id, Double score) {
        this.id = id;
        this.score = score;
    }
}
