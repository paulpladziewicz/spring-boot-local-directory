package com.paulpladziewicz.fremontmi.services;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3Service {

    private final S3Client s3Client;

    public S3Service() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create("AKIAULUTQ6IMXC2G726J", "MsMEQlb3iHfZLpoRMAfg33MBEOSs7lA6fU4MbKkD");
        Region region = Region.US_EAST_2;
        this.s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(region)
                .build();
    }

    public void uploadFile(String bucketName, String key, Path filePath) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(filePath));
        } catch (S3Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> listFiles(String bucketName) {
        List<String> fileList = new ArrayList<>();
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);

        for (S3Object s3Object : listObjectsV2Response.contents()) {
            fileList.add(s3Object.key());
        }

        return fileList;
    }
}

