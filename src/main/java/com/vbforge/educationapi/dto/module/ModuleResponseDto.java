package com.vbforge.educationapi.dto.module;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ModuleResponseDto {

    private Long id;
    private Long courseId;
    private String courseName;      // avoids extra call on the frontend side
    private String title;
    private String content;
    private Integer orderIndex;
    private int assignmentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}