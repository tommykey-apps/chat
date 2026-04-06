package com.example.chat.service;

import com.example.chat.model.dto.PresignedUrlResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    private final String bucketName = "test-bucket";

    private FileService fileService;

    private FileService createFileService() {
        return new FileService(s3Presigner, bucketName, "", "", "");
    }

    @Test
    void generateUploadUrl_returnsPresignedUrlAndS3Key() throws MalformedURLException {
        fileService = createFileService();
        UUID roomId = UUID.randomUUID();
        String fileName = "photo.png";
        String contentType = "image/png";
        URL expectedUrl = URI.create("https://s3.amazonaws.com/test-bucket/uploads/test-key").toURL();

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(expectedUrl);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        PresignedUrlResponse result = fileService.generateUploadUrl(roomId, fileName, contentType);

        assertThat(result.uploadUrl()).isEqualTo(expectedUrl.toString());
        assertThat(result.s3Key()).startsWith("uploads/" + roomId + "/");
        assertThat(result.s3Key()).endsWith("/" + fileName);

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());

        PutObjectPresignRequest captured = captor.getValue();
        assertThat(captured.signatureDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(captured.putObjectRequest().bucket()).isEqualTo(bucketName);
        assertThat(captured.putObjectRequest().contentType()).isEqualTo(contentType);
        assertThat(captured.putObjectRequest().key()).startsWith("uploads/" + roomId + "/");
    }

    @Test
    void generateUploadUrl_generatesUniqueS3KeysForSameFile() throws MalformedURLException {
        fileService = createFileService();
        UUID roomId = UUID.randomUUID();
        URL dummyUrl = URI.create("https://s3.amazonaws.com/test-bucket/dummy").toURL();

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(dummyUrl);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        PresignedUrlResponse result1 = fileService.generateUploadUrl(roomId, "file.txt", "text/plain");
        PresignedUrlResponse result2 = fileService.generateUploadUrl(roomId, "file.txt", "text/plain");

        assertThat(result1.s3Key()).isNotEqualTo(result2.s3Key());
    }

    @Test
    void generateDownloadUrl_returnsPresignedGetUrl() throws MalformedURLException {
        fileService = createFileService();
        String s3Key = "uploads/some-room/some-uuid/photo.png";
        URL expectedUrl = URI.create("https://s3.amazonaws.com/test-bucket/" + s3Key).toURL();

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(expectedUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        String result = fileService.generateDownloadUrl(s3Key);

        assertThat(result).isEqualTo(expectedUrl.toString());

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());

        GetObjectPresignRequest captured = captor.getValue();
        assertThat(captured.signatureDuration()).isEqualTo(Duration.ofHours(1));
        assertThat(captured.getObjectRequest().bucket()).isEqualTo(bucketName);
        assertThat(captured.getObjectRequest().key()).isEqualTo(s3Key);
    }
}
