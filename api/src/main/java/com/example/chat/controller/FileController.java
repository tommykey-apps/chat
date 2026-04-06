package com.example.chat.controller;

import com.example.chat.model.dto.PresignedUrlResponse;
import com.example.chat.service.FileService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/presign-upload")
    public PresignedUrlResponse presignUpload(@RequestBody Map<String, String> request) {
        UUID roomId = UUID.fromString(request.get("roomId"));
        String fileName = request.get("fileName");
        String contentType = request.get("contentType");
        return fileService.generateUploadUrl(roomId, fileName, contentType);
    }

    @GetMapping("/presign-download/**")
    public Map<String, String> presignDownload(jakarta.servlet.http.HttpServletRequest request) {
        String s3Key = request.getRequestURI().replaceFirst("/api/files/presign-download/", "");
        String downloadUrl = fileService.generateDownloadUrl(s3Key);
        return Map.of("downloadUrl", downloadUrl);
    }

    @GetMapping("/presign-download-thumb/**")
    public Map<String, String> presignDownloadThumb(jakarta.servlet.http.HttpServletRequest request) {
        String s3Key = request.getRequestURI().replaceFirst("/api/files/presign-download-thumb/", "");
        String downloadUrl = fileService.generateThumbnailDownloadUrl(s3Key);
        return Map.of("downloadUrl", downloadUrl);
    }
}
