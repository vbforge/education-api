package com.vbforge.educationapi.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PasswordResetToken {

    private static final int EXPIRATION_MINUTES = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = Student.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "student_id")
    private Student student;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public static PasswordResetToken create(Student student, String token) {
        return PasswordResetToken.builder()
                .token(token)
                .student(student)
                .expiryDate(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES))
                .build();
    }
}