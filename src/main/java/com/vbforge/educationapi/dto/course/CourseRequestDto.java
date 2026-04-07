package com.vbforge.educationapi.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseRequestDto {

    @NotBlank(message = "Course name is required")
    @Size(max = 150, message = "Name must be 150 characters or less")
    private String name;

    @Size(max = 2000, message = "Description must be 2000 characters or less")
    private String description;

    @Size(max = 100, message = "Instructor name must be 100 characters or less")
    private String instructor;

    @Size(max = 5000, message = "Syllabus must be 5000 characters or less")
    private String syllabus;

    @Size(max = 500, message = "Schedule must be 500 characters or less")
    private String schedule;
}