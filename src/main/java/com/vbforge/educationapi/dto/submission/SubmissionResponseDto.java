package com.vbforge.educationapi.dto.submission;

import com.vbforge.educationapi.domain.SubmissionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SubmissionResponseDto {

    private Long id;
    private Long assignmentId;
    private String assignmentTitle;
    private Long studentId;
    private String studentName;
    private LocalDateTime submittedAt;
    private String filePath;
    private BigDecimal score;
    private String feedback;
    private SubmissionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}