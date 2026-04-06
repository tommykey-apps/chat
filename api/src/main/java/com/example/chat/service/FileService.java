package com.example.chat.service;

import com.example.chat.model.dto.PresignedUrlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String cloudfrontDomain;
    private final String keyPairId;
    private final String privateKeyPath;

    public FileService(S3Presigner s3Presigner,
                       @Value("${app.s3.bucket}") String bucketName,
                       @Value("${app.cloudfront.domain:}") String cloudfrontDomain,
                       @Value("${app.cloudfront.key-pair-id:}") String keyPairId,
                       @Value("${app.cloudfront.private-key-path:}") String privateKeyPath) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.cloudfrontDomain = cloudfrontDomain;
        this.keyPairId = keyPairId;
        this.privateKeyPath = privateKeyPath;
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
        if (!cloudfrontDomain.isBlank() && !keyPairId.isBlank() && !privateKeyPath.isBlank()) {
            return generateCloudFrontSignedUrl(s3Key);
        }
        return generateS3PresignedUrl(s3Key);
    }

    private String generateCloudFrontSignedUrl(String s3Key) {
        try {
            String resourceUrl = "https://" + cloudfrontDomain + "/" + s3Key;
            Instant expiry = Instant.now().plus(Duration.ofHours(1));

            CannedSignerRequest request = CannedSignerRequest.builder()
                    .resourceUrl(resourceUrl)
                    .privateKey(Path.of(privateKeyPath))
                    .keyPairId(keyPairId)
                    .expirationDate(expiry)
                    .build();

            String signedUrl = CloudFrontUtilities.create()
                    .getSignedUrlWithCannedPolicy(request)
                    .url();

            log.debug("Generated CloudFront signed URL for {}", s3Key);
            return signedUrl;
        } catch (Exception e) {
            log.warn("CloudFront signing failed, falling back to S3 presigned URL: {}", e.getMessage());
            return generateS3PresignedUrl(s3Key);
        }
    }

    private String generateS3PresignedUrl(String s3Key) {
        var getObjectRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        var presignRequest = software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getObjectRequest)
                .build();

        var presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }
}
