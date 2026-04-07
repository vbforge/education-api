package com.vbforge.educationapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class StorageService {

    // later will implement real file saving to disk
    public String store(MultipartFile file, Long studentId, Long assignmentId) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        // placeholder — returns a fake path for now
        String filename = "student_%d_assignment_%d_%s"
                .formatted(studentId, assignmentId, file.getOriginalFilename());
        log.info("(stub) Would store file: {}", filename);
        return "uploads/" + filename;
    }
}