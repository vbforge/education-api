package com.vbforge.educationapi.service;

import com.vbforge.educationapi.config.StorageProperties;
import com.vbforge.educationapi.exception.StorageException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageProperties storageProperties;
    private Path rootLocation;

    // runs once when the app starts — creates the upload directory if missing
    @PostConstruct
    public void init() {
        rootLocation = Paths.get(storageProperties.getUploadDir());
        try {
            Files.createDirectories(rootLocation);
            log.info("Upload directory ready: {}", rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new StorageException("Could not create upload directory", e);
        }
    }

    // ── Store ────────────────────────────────────────────────────

    public String store(MultipartFile file, Long studentId, Long assignmentId) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        validateFile(file);

        // build a unique filename: {studentId}_{assignmentId}_{uuid}.{ext}
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );
        String extension = getExtension(originalFilename);
        String filename   = "%d_%d_%s.%s".formatted(
                studentId, assignmentId, UUID.randomUUID(), extension
        );

        // organise into subdirectory per student: uploads/student_{id}/filename
        Path studentDir = rootLocation.resolve("student_" + studentId);
        try {
            Files.createDirectories(studentDir);
            Path destination = studentDir.resolve(filename);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            String relativePath = "student_" + studentId + "/" + filename;
            log.info("File stored: {}", relativePath);
            return relativePath;

        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + originalFilename, e);
        }
    }

    // ── Load ─────────────────────────────────────────────────────

    // returns the absolute Path for a stored file — used by FileController
    public Path load(String relativePath) {
        Path file = rootLocation.resolve(relativePath).normalize();

        // security: prevent path traversal attacks (e.g. ../../etc/passwd)
        if (!file.startsWith(rootLocation)) {
            throw new StorageException("Cannot access file outside upload directory");
        }

        if (!Files.exists(file)) {
            throw new StorageException("File not found: " + relativePath);
        }

        return file;
    }

    // ── Delete ───────────────────────────────────────────────────

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path file = load(relativePath);
            Files.deleteIfExists(file);
            log.info("File deleted: {}", relativePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", relativePath, e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        // size check
        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new StorageException(
                    "File exceeds maximum allowed size of %d MB"
                    .formatted(storageProperties.getMaxFileSizeBytes() / 1024 / 1024)
            );
        }

        // extension check
        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "";
        String extension = getExtension(originalFilename).toLowerCase();
        boolean allowed = Arrays.asList(storageProperties.getAllowedExtensions())
                .contains(extension);

        if (!allowed) {
            throw new StorageException(
                    "File type '.%s' is not allowed. Allowed types: %s"
                    .formatted(extension,
                               String.join(", ", storageProperties.getAllowedExtensions()))
            );
        }

        // filename safety check
        String cleanName = StringUtils.cleanPath(originalFilename);
        if (cleanName.contains("..")) {
            throw new StorageException("Filename contains invalid path sequence: " + cleanName);
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0) ? filename.substring(dotIndex + 1) : "";
    }
}