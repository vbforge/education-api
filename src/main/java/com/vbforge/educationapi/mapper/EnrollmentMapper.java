package com.vbforge.educationapi.mapper;

import com.vbforge.educationapi.domain.Enrollment;
import com.vbforge.educationapi.dto.enrollment.EnrollmentResponseDto;

public class EnrollmentMapper {

    private EnrollmentMapper() {}

    public static EnrollmentResponseDto toDto(Enrollment enrollment) {
        return EnrollmentResponseDto.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent().getName())
                .courseId(enrollment.getCourse().getId())
                .courseName(enrollment.getCourse().getName())
                .status(enrollment.getStatus())
                .grade(enrollment.getGrade())
                .progressPct(enrollment.getProgressPct())
                .createdAt(enrollment.getCreatedAt())
                .updatedAt(enrollment.getUpdatedAt())
                .build();
    }
}