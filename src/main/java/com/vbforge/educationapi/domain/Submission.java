package com.vbforge.educationapi.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Submission extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column
    private LocalDateTime submittedAt;

    @Size(max = 500)
    @Column(length = 500)
    private String filePath;               // path to uploaded file

    @DecimalMin("0.0") @DecimalMax("100.0")
    @Column(precision = 5, scale = 2)
    private BigDecimal score;              // null until graded

    @Size(max = 1000)
    @Column(length = 1000)
    private String feedback;              // instructor feedback

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

}
