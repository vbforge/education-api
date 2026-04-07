package com.vbforge.educationapi.dto.assignment;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AssignmentResponseDto {

    private Long id;
    private Long moduleId;
    private String moduleTitle;
    private Long courseId;          // convenient for breadcrumb navigation
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private Integer pointsPossible;
    private int submissionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}