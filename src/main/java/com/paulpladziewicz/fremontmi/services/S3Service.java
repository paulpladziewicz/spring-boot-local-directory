package com.paulpladziewicz.fremontmi.services;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


@Service
public class S3Service {

    private static S3Client s3;

    private static String bucketName = "fremontmi";

    private static String key = "fremontmi";


    public void upload() throws IOException {
        Region region = Region.US_EAST_2;
        s3 = S3Client.builder()
                .region(region)
                .build();

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3.putObject(objectRequest, RequestBody.fromByteBuffer(getRandomByteBuffer(10_000)));

    }

    private static ByteBuffer getRandomByteBuffer(int size) throws IOException {
        byte[] b = new byte[size];
        new Random().nextBytes(b);
        return ByteBuffer.wrap(b);
    }
}
