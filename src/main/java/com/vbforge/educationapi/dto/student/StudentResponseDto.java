package com.vbforge.educationapi.dto.student;

import com.vbforge.educationapi.domain.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudentResponseDto {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private int enrollmentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // passwordHash is intentionally excluded — never expose it
}