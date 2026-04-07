package com.vbforge.educationapi.api;

import com.vbforge.educationapi.dto.enrollment.EnrollmentRequestDto;
import com.vbforge.educationapi.dto.enrollment.EnrollmentResponseDto;
import com.vbforge.educationapi.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Validated
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // GET /api/v1/enrollments/student/{studentId}
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<EnrollmentResponseDto>> getByStudent(
            @PathVariable Long studentId
    ) {
        return ResponseEntity.ok(enrollmentService.findByStudent(studentId));
    }

    // GET /api/v1/enrollments/course/{courseId}
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<EnrollmentResponseDto>> getByCourse(
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(enrollmentService.findByCourse(courseId));
    }

    // POST /api/v1/enrollments
    @PostMapping
    public ResponseEntity<EnrollmentResponseDto> enroll(
            @Valid @RequestBody EnrollmentRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enroll(dto));
    }

    // PATCH /api/v1/enrollments/drop?studentId=1&courseId=2
    // PATCH because we're partially updating status, not replacing the resource
    @PatchMapping("/drop")
    public ResponseEntity<EnrollmentResponseDto> drop(
            @RequestParam Long studentId,
            @RequestParam Long courseId
    ) {
        return ResponseEntity.ok(enrollmentService.drop(studentId, courseId));
    }

    // PATCH /api/v1/enrollments/finalize?studentId=1&courseId=2
    @PatchMapping("/finalize")
    public ResponseEntity<EnrollmentResponseDto> finalize(
            @RequestParam Long studentId,
            @RequestParam Long courseId
    ) {
        return ResponseEntity.ok(enrollmentService.finalizeGrade(studentId, courseId));
    }

    // PATCH /api/v1/enrollments/grade?studentId=1&courseId=2&grade=85.50
    @PatchMapping("/grade")
    public ResponseEntity<EnrollmentResponseDto> updateGrade(
            @RequestParam Long studentId,
            @RequestParam Long courseId,
            @RequestParam BigDecimal grade
    ) {
        return ResponseEntity.ok(enrollmentService.updateGrade(studentId, courseId, grade));
    }

}













