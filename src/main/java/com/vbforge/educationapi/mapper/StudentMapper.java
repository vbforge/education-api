package com.vbforge.educationapi.mapper;

import com.vbforge.educationapi.domain.Student;
import com.vbforge.educationapi.dto.student.StudentResponseDto;

public class StudentMapper {

    private StudentMapper() {}

    public static StudentResponseDto toDto(Student student, int enrollmentCount) {
        return StudentResponseDto.builder()
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .role(student.getRole())
                .enrollmentCount(enrollmentCount)
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
        // passwordHash intentionally excluded
    }
}