package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.repositories.TagAutocompleteRepository;
import com.paulpladziewicz.fremontmi.repositories.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;

    private final TagAutocompleteRepository tagAutocompleteRepository;

    public TagService(TagRepository tagRepository, TagAutocompleteRepository tagAutocompleteRepository) {
        this.tagRepository = tagRepository;
        this.tagAutocompleteRepository = tagAutocompleteRepository;
    }

    public List<String> autocompleteList(String text) {

        return tagAutocompleteRepository.searchTagsByName(text);
    }
}
