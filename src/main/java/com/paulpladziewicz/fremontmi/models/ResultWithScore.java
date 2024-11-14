package com.paulpladziewicz.fremontmi.models;

public class ResultWithScore {
    private String id;
    private Double score;

    public ResultWithScore(String id, Double score) {
        this.id = id;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}

