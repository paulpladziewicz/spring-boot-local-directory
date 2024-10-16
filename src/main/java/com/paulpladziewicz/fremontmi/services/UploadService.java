package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.exceptions.FileUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

@Service
public class UploadService {

    private final S3Client s3Client;

    private final String bucketName = "fremontmi";

    public UploadService() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create("AKIAULUTQ6IMXC2G726J", "MsMEQlb3iHfZLpoRMAfg33MBEOSs7lA6fU4MbKkD");
        Region region = Region.US_EAST_2;
        this.s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(region)
                .build();
    }

    public void uploadFile(MultipartFile file, String fileName) throws IOException {
        try {
            String contentType = file.getContentType();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        } catch (S3Exception e) {
            throw new FileUploadException("Failed to upload file to S3: " + fileName, e);
        }
    }
}

