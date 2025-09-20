package com.paulpladziewicz.fremontmi.content;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "content_vectors")
public class ContentVector {

    @Id
    private String contentId;

    private String inputText;

    private List<Double> vector;
}
