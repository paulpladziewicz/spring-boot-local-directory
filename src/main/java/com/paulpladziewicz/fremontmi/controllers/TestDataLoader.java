package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Tag;
import com.paulpladziewicz.fremontmi.repositories.TagRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class TestDataLoader implements CommandLineRunner {


    private final TagRepository tagRepository;

    public TestDataLoader(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        if (tagRepository.count() == 0) {
            System.out.println("Inserting test tags into MongoDB.");
            tagRepository.saveAll(Arrays.asList(
                new Tag("home", "Home"),
                new Tag("yard", "Yard"),
                new Tag("dogwalking", "Dog Walking"),
                new Tag("langaugelearning", "Language Learning")
            ));
        } else {
            System.out.println("Test tags already exist, skipping insertion.");
        }
    }
}
