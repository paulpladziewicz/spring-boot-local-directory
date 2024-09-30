package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Content;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContentService {

    private final ContentRepository contentRepository;

    public ContentService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public List<String> getAllContentEntityUrls() {
        List<Content> contentEntities = contentRepository.findAll();

        return contentEntities.stream()
                .map(contentEntity -> "https://fremontmi.com" + contentEntity.getPathname())
                .collect(Collectors.toList());
    }
}
