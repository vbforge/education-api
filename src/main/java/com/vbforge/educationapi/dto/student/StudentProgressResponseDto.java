package com.vbforge.educationapi.dto.student;

import com.vbforge.educationapi.dto.enrollment.EnrollmentResponseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StudentProgressResponseDto {

    private Long   studentId;
    private String studentName;
    private int    totalEnrollments;
    private int    completedCourses;
    private int    activeCourses;
    private List<EnrollmentResponseDto> enrollments;
}