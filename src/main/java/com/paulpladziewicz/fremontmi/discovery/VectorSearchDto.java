package com.paulpladziewicz.fremontmi.discovery;

import com.paulpladziewicz.fremontmi.content.ContentType;

import lombok.Data;

@Data
public class VectorSearchDto {
    private ContentType type;
    private String pathname;
    private String title;
    private String description;

    public VectorSearchDto(ContentType type, String pathname, String title, String description) {
        this.type = type;
        this.pathname = pathname;
        this.title = title;
        this.description = description;
    }
}
