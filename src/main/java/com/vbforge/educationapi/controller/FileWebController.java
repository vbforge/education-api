package com.vbforge.educationapi.controller;

import com.vbforge.educationapi.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.MalformedURLException;
import java.nio.file.Path;

@Slf4j
@Controller
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileWebController {

    private final StorageService storageService;

    @GetMapping("/{subDir}/{filename}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String subDir,
            @PathVariable String filename
    ) {
        try {
            String relativePath = subDir + "/" + filename;
            log.info("Downloading file: {}", relativePath);

            Path filePath = storageService.load(relativePath);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.error("File not found or not readable: {}", relativePath);
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = "application/octet-stream";
            try {
                String detectedType = java.nio.file.Files.probeContentType(filePath);
                if (detectedType != null) {
                    contentType = detectedType;
                }
            } catch (Exception e) {
                log.warn("Could not determine content type for: {}", relativePath);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("Malformed URL: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

}
