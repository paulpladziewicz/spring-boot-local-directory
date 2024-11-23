package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.Content;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ContentArchiveRepository {
    private final MongoTemplate mongoTemplate;

    public ContentArchiveRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void save(Content content) {
        content.setVersion(null);
        mongoTemplate.save(content, "content_archive");
    }
}
