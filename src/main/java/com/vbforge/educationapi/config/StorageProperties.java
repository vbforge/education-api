package com.vbforge.educationapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    // base directory where all uploads are saved
    private String uploadDir = "uploads";

    // allowed file extensions
    private String[] allowedExtensions = {"pdf", "doc", "docx", "txt", "zip", "png", "jpg"};

    // max file size in bytes (default 10MB)
    private long maxFileSizeBytes = 10 * 1024 * 1024L;
}