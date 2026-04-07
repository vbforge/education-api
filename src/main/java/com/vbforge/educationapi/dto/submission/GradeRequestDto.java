package com.vbforge.educationapi.dto.submission;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class GradeRequestDto {

    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.0", message = "Score must be 0 or more")
    @DecimalMax(value = "100.0", message = "Score must be 100 or less")
    private BigDecimal score;

    private String feedback;    // optional instructor comment
}