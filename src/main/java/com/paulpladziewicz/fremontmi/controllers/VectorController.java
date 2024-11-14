package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Content;
import com.paulpladziewicz.fremontmi.models.VectorSearchDto;
import com.paulpladziewicz.fremontmi.services.VectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vectors")
public class VectorController {

    private final VectorService vectorService;

    public VectorController(VectorService vectorService) {
        this.vectorService = vectorService;
    }

    @PostMapping("/search")
    public ResponseEntity<List<VectorSearchDto>> searchRelevantContent(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");

        List<Content> results = vectorService.searchRelevantContent(prompt);

        List<VectorSearchDto> dtos = results.stream()
                .map(this::convertToDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<List<Double>> generateVector(@PathVariable String id) {
        List<Double> vector = vectorService.generateVector(id);
        return ResponseEntity.ok(vector);
    }

    private VectorSearchDto convertToDTO(Content content) {
        return new VectorSearchDto(
                content.getType(),
                content.getPathname(),
                content.getDetail().getTitle(),
                content.getDetail().getDescription()
        );
    }
}
