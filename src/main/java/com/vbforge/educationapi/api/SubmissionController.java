package com.vbforge.educationapi.api;

import com.vbforge.educationapi.dto.submission.SubmissionRequestDto;
import com.vbforge.educationapi.dto.submission.SubmissionResponseDto;
import com.vbforge.educationapi.service.SubmissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    // GET /api/v1/submissions/assignment/{assignmentId}
    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<List<SubmissionResponseDto>> getByAssignment(
            @PathVariable Long assignmentId
    ) {
        return ResponseEntity.ok(submissionService.findByAssignment(assignmentId));
    }

    // GET /api/v1/submissions/student/{studentId}
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<SubmissionResponseDto>> getByStudent(
            @PathVariable Long studentId
    ) {
        return ResponseEntity.ok(submissionService.findByStudent(studentId));
    }

    // GET /api/v1/submissions/{id}
    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.findById(id));
    }

    // POST /api/v1/submissions
    // multipart/form-data: assignmentId + studentId + file
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<SubmissionResponseDto> submit(
            @Valid @ModelAttribute SubmissionRequestDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        // file handling will be wired in Phase 4 (file upload)
        // for now we pass null as filePath — service handles null safely
        String filePath = null;
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(submissionService.submit(dto, filePath));
    }

    // PATCH /api/v1/submissions/{id}/grade
    @PatchMapping("/{id}/grade")
    public ResponseEntity<SubmissionResponseDto> grade(
            @PathVariable Long id,
            @RequestParam
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal score,
            @RequestParam(required = false) String feedback
    ) {
        return ResponseEntity.ok(submissionService.grade(id, score, feedback));
    }
}