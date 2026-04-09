package com.vbforge.educationapi.mapper;

import com.vbforge.educationapi.domain.Assignment;
import com.vbforge.educationapi.dto.assignment.AssignmentRequestDto;
import com.vbforge.educationapi.dto.assignment.AssignmentResponseDto;

public class AssignmentMapper {

    private AssignmentMapper() {}

    public static AssignmentResponseDto toDto(Assignment assignment, int submissionCount) {
        return AssignmentResponseDto.builder()
                .id(assignment.getId())
                .moduleId(assignment.getModule().getId())
                .moduleTitle(assignment.getModule().getTitle())
                .courseId(assignment.getModule().getCourse().getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .dueDate(assignment.getDueDate())
                .pointsPossible(assignment.getPointsPossible())
                .submissionCount(submissionCount)
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }

    public static void updateEntity(Assignment assignment, AssignmentRequestDto dto) {
        assignment.setTitle(dto.getTitle());
        assignment.setDescription(dto.getDescription());
        assignment.setDueDate(dto.getDueDate());
        assignment.setPossiblePoints(dto.getPointsPossible() != null ? dto.getPointsPossible() : 100);
    }
}