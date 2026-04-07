package com.vbforge.educationapi.dto.enrollment;

import com.vbforge.educationapi.domain.EnrollmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class EnrollmentResponseDto {

    private Long id;
    private Long studentId;
    private String studentName;
    private Long courseId;
    private String courseName;
    private EnrollmentStatus status;
    private BigDecimal grade;
    private BigDecimal progressPct;
    private LocalDateTime createdAt;    // this is effectively the enrollment date
    private LocalDateTime updatedAt;
}