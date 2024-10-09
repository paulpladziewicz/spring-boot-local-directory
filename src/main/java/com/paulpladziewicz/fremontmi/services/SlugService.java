package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Content;
import com.paulpladziewicz.fremontmi.repositories.ContentRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SlugService {

    private final ContentRepository contentRepository;

    public SlugService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public String createUniqueSlug(String name, String contentType) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String baseSlug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        List<Content> matchingSlugs = contentRepository.findBySlugRegexAndType("^" + baseSlug + "(-\\d+)?$", contentType);

        return generateSlugFromMatches(baseSlug, matchingSlugs);
    }

    private String generateSlugFromMatches(String baseSlug, List<Content> matchingSlugs) {
        if (matchingSlugs.isEmpty()) {
            return baseSlug;
        }

        Pattern pattern = Pattern.compile(Pattern.quote(baseSlug) + "-(\\d+)$");
        int maxNumber = 0;
        boolean baseSlugExists = false;

        for (Content content : matchingSlugs) {
            String slug = content.getSlug();
            if (slug.equals(baseSlug)) {
                baseSlugExists = true;
            }

            Matcher matcher = pattern.matcher(slug);
            if (matcher.find()) {
                maxNumber = Math.max(maxNumber, Integer.parseInt(matcher.group(1)));
            }
        }

        if (baseSlugExists || maxNumber > 0) {
            return baseSlug + "-" + (maxNumber + 1);
        }

        return baseSlug;
    }
}
