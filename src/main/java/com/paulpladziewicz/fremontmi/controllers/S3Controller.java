package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.services.S3Service;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.List;

@Controller
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    private final String bucketName = "fremontmi";

//    @GetMapping("/s3")
//    public String index(Model model) {
//        List<String> files = s3Service.listFiles(bucketName);
//        model.addAttribute("files", files);
//        return "s3";
//    }
//
//    @PostMapping("/upload")
//    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) throws IOException {
//        Path tempFile = Files.createTempFile("temp", file.getOriginalFilename());
//        file.transferTo(tempFile.toFile());
//
//        s3Service.uploadFile(bucketName, file.getOriginalFilename(), tempFile);
//
//        return "redirect:/s3";
//    }
}
