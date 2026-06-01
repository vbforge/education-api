package com.vbforge.educationapi.service;

import com.vbforge.educationapi.domain.*;
import com.vbforge.educationapi.domain.Module;
import com.vbforge.educationapi.repository.AssignmentRepository;
import com.vbforge.educationapi.repository.EnrollmentRepository;
import com.vbforge.educationapi.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock(lenient = true)
    private SubmissionRepository submissionRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProgressService progressService;

    private Enrollment testEnrollment;
    private Submission gradedSubmission;
    private Course testCourse;
    private Student testStudent;
    private Module testModule;
    private Assignment testAssignment;

    @BeforeEach
    void setUp() {
        // Create Course (extends BaseEntity)
        testCourse = Course.builder()
                .name("Test Course")
                .description("Test Description")
                .instructor("instructor@email.com")
                .build();
        testCourse.setId(1L);

        // Create Student (extends BaseEntity)
        testStudent = Student.builder()
                .name("Test Student")
                .email("student@email.com")
                .passwordHash("hashedPassword")
                .role(Role.STUDENT)
                .build();
        testStudent.setId(1L);

        // Create Module (extends BaseEntity)
        testModule = Module.builder()
                .course(testCourse)
                .title("Test Module")
                .orderIndex(1)
                .build();
        testModule.setId(1L);

        // Create Assignment (extends BaseEntity)
        testAssignment = Assignment.builder()
                .module(testModule)
                .title("Test Assignment")
                .pointsPossible(100)
                .dueDate(LocalDateTime.now().plusDays(7))
                .build();
        testAssignment.setId(1L);

        // Create Enrollment (extends BaseEntity)
        testEnrollment = Enrollment.builder()
                .student(testStudent)
                .course(testCourse)
                .progressPct(BigDecimal.ZERO)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        testEnrollment.setId(1L);

        // Create Submission (extends BaseEntity)
        gradedSubmission = Submission.builder()
                .assignment(testAssignment)
                .student(testStudent)
                .score(BigDecimal.valueOf(85))
                .status(SubmissionStatus.GRADED)
                .submittedAt(LocalDateTime.now())
                .build();
        gradedSubmission.setId(1L);
    }

    @Test
    void recalculate_UpdatesProgressCorrectly() {
        when(enrollmentRepository.findByStudentIdAndCourseId(anyLong(), anyLong()))
                .thenReturn(Optional.of(testEnrollment));
        when(assignmentRepository.findByCourseId(anyLong()))
                .thenReturn(List.of(testAssignment));
        when(submissionRepository.findByStudentId(anyLong()))
                .thenReturn(List.of(gradedSubmission));

        progressService.recalculate(1L, 1L);

        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void recalculate_NoEnrollment_DoesNothing() {
        when(enrollmentRepository.findByStudentIdAndCourseId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        progressService.recalculate(1L, 1L);

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Disabled
    @Test
    void recalculate_EnrollmentDropped_DoesNothing() {
        testEnrollment.setStatus(EnrollmentStatus.DROPPED);
        when(enrollmentRepository.findByStudentIdAndCourseId(anyLong(), anyLong()))
                .thenReturn(Optional.of(testEnrollment));

        progressService.recalculate(1L, 1L);

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void finalizeGrade_SetsCorrectGrade() {
        when(enrollmentRepository.findByStudentIdAndCourseId(anyLong(), anyLong()))
                .thenReturn(Optional.of(testEnrollment));
        when(submissionRepository.findByStudentId(anyLong()))
                .thenReturn(List.of(gradedSubmission));

        progressService.finalizeGrade(1L, 1L);

        verify(enrollmentRepository).save(any(Enrollment.class));
    }
}