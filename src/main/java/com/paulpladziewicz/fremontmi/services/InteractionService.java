package com.paulpladziewicz.fremontmi.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InteractionService {

    private static final Logger logger = LoggerFactory.getLogger(InteractionService.class);
    private final ContentService contentService;


    public InteractionService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void heart(String contentId) {

    }

    public void bookmark(String contentId) {

    }

    public void join(String contentId) {

    }

    public void leave(String contentId) {

    }

    public void cancel(String contentId) {

    }

    public void reverseCancel(String contentId) {

    }
}
