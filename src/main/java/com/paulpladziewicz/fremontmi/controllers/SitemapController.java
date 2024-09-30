package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.services.ContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@RestController
public class SitemapController {

    private final ContentService contentService;

    public SitemapController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    public void generateSitemap(HttpServletResponse response) throws IOException {
        List<String> urls = contentService.getAllContentEntityUrls();

        response.setContentType("application/xml");
        response.setStatus(HttpServletResponse.SC_OK); // Optional: Explicitly set success status
        PrintWriter writer = response.getWriter();

        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        // Add your main pages
        writer.println("<url><loc>https://fremontmi.com/</loc><changefreq>daily</changefreq><priority>1.0</priority></url>");

        // Add URLs from content entities
        for (String url : urls) {
            writer.println("<url>");
            writer.println("<loc>" + url + "</loc>");
            writer.println("<changefreq>weekly</changefreq>");
            writer.println("<priority>0.5</priority>");
            writer.println("</url>");
        }

        writer.println("</urlset>");
        writer.flush();
    }
}