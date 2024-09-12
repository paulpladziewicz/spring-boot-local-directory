package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.NeighborService;
import com.paulpladziewicz.fremontmi.models.NeighborServiceProfile;
import com.paulpladziewicz.fremontmi.repositories.NeighborServiceProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class TestDataLoader implements CommandLineRunner {

    private final NeighborServiceProfileRepository profileRepository;

    public TestDataLoader(NeighborServiceProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (profileRepository.count() == 0) {

            // Create Neighbor Services for each profile
            NeighborService service1 = new NeighborService();
            service1.setName("Lawn Mowing");
            service1.setDescription("Professional lawn mowing service");
            service1.setCreatedAt(LocalDateTime.now());
            service1.setUpdatedAt(LocalDateTime.now());

            NeighborService service2 = new NeighborService();
            service2.setName("House Cleaning");
            service2.setDescription("Thorough house cleaning service");
            service2.setCreatedAt(LocalDateTime.now());
            service2.setUpdatedAt(LocalDateTime.now());

            NeighborService service3 = new NeighborService();
            service3.setName("Dog Walking");
            service3.setDescription("Daily dog walking service");
            service3.setCreatedAt(LocalDateTime.now());
            service3.setUpdatedAt(LocalDateTime.now());

            // Create Profiles with embedded Neighbor Services
            NeighborServiceProfile profile1 = new NeighborServiceProfile();
            profile1.setFirstName("John");
            profile1.setLastName("Doe");
            profile1.setEmail("john.doe@example.com");
            profile1.setDescription("Providing top-notch services in the neighborhood.");
            profile1.setTags(Arrays.asList("home", "yard", "pet"));
            profile1.setNeighborServices(Arrays.asList(service1, service2, service3));
            profile1.setStatus("active");
            profile1.setCreatedAt(LocalDateTime.now());
            profile1.setUpdatedAt(LocalDateTime.now());

            NeighborServiceProfile profile2 = new NeighborServiceProfile();
            profile2.setFirstName("Sarah");
            profile2.setLastName("Smith");
            profile2.setEmail("sarah.smith@example.com");
            profile2.setDescription("Offering a range of household services.");
            profile2.setTags(Arrays.asList("cleaning", "yard"));
            profile2.setNeighborServices(Arrays.asList(service1, service2));
            profile2.setStatus("active");
            profile2.setCreatedAt(LocalDateTime.now());
            profile2.setUpdatedAt(LocalDateTime.now());

            NeighborServiceProfile profile3 = new NeighborServiceProfile();
            profile3.setFirstName("Mike");
            profile3.setLastName("Johnson");
            profile3.setEmail("mike.johnson@example.com");
            profile3.setDescription("Specializes in yard and pet care services.");
            profile3.setTags(Arrays.asList("pet", "yard"));
            profile3.setNeighborServices(Arrays.asList(service3));
            profile3.setStatus("active");
            profile3.setCreatedAt(LocalDateTime.now());
            profile3.setUpdatedAt(LocalDateTime.now());

            // Save Profiles
            profileRepository.saveAll(Arrays.asList(profile1, profile2, profile3));

            System.out.println("Test data inserted into MongoDB.");
        } else {
            System.out.println("Test data already exists, skipping insertion.");
        }
    }
}
