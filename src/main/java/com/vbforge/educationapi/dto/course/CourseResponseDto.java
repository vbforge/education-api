package com.vbforge.educationapi.dto.course;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CourseResponseDto {

    private Long id;
    private String name;
    private String description;
    private String instructor;
    private String syllabus;
    private String schedule;
    private int moduleCount;        // handy for UI — no need to load modules
    private int enrollmentCount;    // handy for UI — no need to load students
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}