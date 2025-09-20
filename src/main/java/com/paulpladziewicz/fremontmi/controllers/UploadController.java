package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.content.Content;
import com.paulpladziewicz.fremontmi.content.ContentService;
import com.paulpladziewicz.fremontmi.content.NeighborServicesProfile;
import com.paulpladziewicz.fremontmi.content.UploadService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Controller
public class UploadController {

    private final UploadService uploadService;
    private final ContentService contentService;

    public UploadController(UploadService uploadService, ContentService contentService) {
        this.uploadService = uploadService;
        this.contentService = contentService;
    }

    @PostMapping("/upload")
    public String uploadFiles(@RequestParam("files") List<MultipartFile> files, @RequestParam("contentType") String contentType, @RequestParam("contentId") String contentId) throws IOException {
        Content content = contentService.findById(contentId);
        contentService.checkPermission(content);


        CompletableFuture<?>[] uploadFutures = files.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> {
                    String uniqueId = UUID.randomUUID().toString();
                    String fileName = contentType + "_" + contentId + "_" + uniqueId + getFileExtension(file.getContentType());
                    return CompletableFuture.runAsync(() -> {
                        try {
                            uploadService.uploadFile(file, fileName);
                            String cdnUrl = "https://cdn.fremontmi.com/" + fileName;
                            if (content.getDetail() instanceof NeighborServicesProfile detail) {
                                detail.setProfileImageFileName(fileName);
                                detail.setProfileImageUrl(cdnUrl);
                                contentService.save(content);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }).toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(uploadFutures).join();

        return "redirect:/my/neighbor-services-profile";
    }

    private String getFileExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ""; // No extension if the content type is unrecognized
        };
    }
}
