package com.vbforge.educationapi.dto.module;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ModuleRequestDto {

    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotBlank(message = "Module title is required")
    @Size(max = 150, message = "Title must be 150 characters or less")
    private String title;

    @Size(max = 5000, message = "Content must be 5000 characters or less")
    private String content;

    @Min(value = 0, message = "Order index must be zero or positive")
    private Integer orderIndex = 0;
}