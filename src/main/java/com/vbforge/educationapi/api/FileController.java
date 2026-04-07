package com.vbforge.educationapi.api;

import com.vbforge.educationapi.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final StorageService storageService;

    // GET /api/v1/files/{studentId}/{filename}
    // e.g. GET /api/v1/files/student_1/1_2_uuid.pdf
    @GetMapping("/{subDir}/{filename}")
    public ResponseEntity<Resource> download(
            @PathVariable String subDir,
            @PathVariable String filename
    ) {
        String relativePath = subDir + "/" + filename;
        Path   filePath     = storageService.load(relativePath);

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // try to determine content type — fallback to binary stream
            String contentType = "application/octet-stream";
            try {
                contentType = java.nio.file.Files
                        .probeContentType(filePath);
                if (contentType == null) contentType = "application/octet-stream";
            } catch (Exception e) {
                log.warn("Could not determine content type for: {}", relativePath);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\""
                    )
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}