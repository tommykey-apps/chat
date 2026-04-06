package com.example.chat.service;

import com.example.chat.model.dto.PresignedUrlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
public class FileService {

    private final S3Presigner s3Presigner;
    private final String bucketName;

    public FileService(S3Presigner s3Presigner,
                       @Value("${app.s3.bucket}") String bucketName) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    public PresignedUrlResponse generateUploadUrl(UUID roomId, String fileName, String contentType) {
        String s3Key = "uploads/%s/%s/%s".formatted(roomId, UUID.randomUUID(), fileName);

        var putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        var presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest)
                .build();

        var presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return new PresignedUrlResponse(presignedRequest.url().toString(), s3Key);
    }

    public String getThumbnailKey(String s3Key) {
        return s3Key.replaceAll("(\\.[^.]+)$", "_thumb$1");
    }

    public String generateThumbnailDownloadUrl(String s3Key) {
        return generateDownloadUrl(getThumbnailKey(s3Key));
    }

    public String generateDownloadUrl(String s3Key) {
        var getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        var presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getObjectRequest)
                .build();

        var presignedRequest = s3Presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }
}
