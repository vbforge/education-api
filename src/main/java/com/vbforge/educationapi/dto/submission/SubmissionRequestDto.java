package com.vbforge.educationapi.dto.submission;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubmissionRequestDto {

    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;

    @NotNull(message = "Student ID is required")
    private Long studentId;

    // filePath is set in service layer after file is saved to disk
    // score and feedback are set during grading — not on initial submission
}