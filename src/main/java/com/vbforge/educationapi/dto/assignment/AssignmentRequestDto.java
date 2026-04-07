package com.vbforge.educationapi.dto.assignment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AssignmentRequestDto {

    @NotNull(message = "Module ID is required")
    private Long moduleId;

    @NotBlank(message = "Assignment title is required")
    @Size(max = 150, message = "Title must be 150 characters or less")
    private String title;

    @Size(max = 2000, message = "Description must be 2000 characters or less")
    private String description;

    private LocalDateTime dueDate;

    @Min(value = 0, message = "Points must be zero or more")
    private Integer pointsPossible = 100;
}