package com.vbforge.educationapi.mapper;

import com.vbforge.educationapi.domain.Submission;
import com.vbforge.educationapi.dto.submission.SubmissionResponseDto;

public class SubmissionMapper {

    private SubmissionMapper() {}

    public static SubmissionResponseDto toDto(Submission submission) {
        return SubmissionResponseDto.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .assignmentTitle(submission.getAssignment().getTitle())
                .studentId(submission.getStudent().getId())
                .studentName(submission.getStudent().getName())
                .submittedAt(submission.getSubmittedAt())
                .filePath(submission.getFilePath())
                .score(submission.getScore())
                .feedback(submission.getFeedback())
                .status(submission.getStatus())
                .createdAt(submission.getCreatedAt())
                .updatedAt(submission.getUpdatedAt())
                .build();
    }
}