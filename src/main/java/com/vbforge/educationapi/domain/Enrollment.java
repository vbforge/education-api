package com.vbforge.educationapi.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"student_id", "course_id"}  // one enrollment per student per course
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    // final grade 0.00 – 100.00, null until course completed
    @DecimalMin("0.0") @DecimalMax("100.0")
    @Column(precision = 5, scale = 2)
    private BigDecimal grade;

    // 0.00 – 100.00 — updated as student completes assignments
    @DecimalMin("0.0") @DecimalMax("100.0")
    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal progressPct = BigDecimal.ZERO;
}
