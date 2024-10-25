package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.exceptions.PermissionDeniedException;
import com.paulpladziewicz.fremontmi.services.ContentService;
import com.paulpladziewicz.fremontmi.services.UploadService;
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

//    @PostMapping("/upload")
//    public String uploadFiles(@RequestParam("files") List<MultipartFile> files, @RequestParam("contentType") String contentType, @RequestParam("contentId") String contentId) throws IOException {
//
//        Boolean hasPermission = switch (contentType) {
//            case "neighbor-services-profile" -> neighborServicesProfileService.hasUploadPermission(contentId);
//            default -> false;
//        };
//
//        if (!hasPermission) {
//            throw new PermissionDeniedException("Does not have permission to upload files");
//        }
//
//        CompletableFuture<?>[] uploadFutures = files.stream()
//                .filter(file -> !file.isEmpty())
//                .map(file -> {
//                    String uniqueId = UUID.randomUUID().toString();
//                    String fileName = contentType + "_" + contentId + "_" + uniqueId + getFileExtension(file.getContentType());
//                    return CompletableFuture.runAsync(() -> {
//                        try {
//                            uploadService.uploadFile(file, fileName);
//                            String cdnUrl = "https://cdn.fremontmi.com/" + fileName;
//                            neighborServicesProfileService.setProfileImageUrl(contentId, cdnUrl, fileName);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    });
//                }).toArray(CompletableFuture[]::new);
//
//        CompletableFuture.allOf(uploadFutures).join();
//
//        return "redirect:/my/neighbor-services/profile";
//    }

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
