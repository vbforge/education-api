package com.vbforge.educationapi.service;

import com.vbforge.educationapi.domain.*;
import com.vbforge.educationapi.repository.AssignmentRepository;
import com.vbforge.educationapi.repository.EnrollmentRepository;
import com.vbforge.educationapi.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProgressService {

    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final NotificationService notificationService;

    // called after every graded submission
    public void recalculate(Long studentId, Long courseId) {
        log.info("Recalculating progress for studentId={}, courseId={}", studentId, courseId);
        
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElse(null);

        if (enrollment == null || enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            log.warn("Skipping progress recalc — enrollment not found or dropped");
            return;
        }

        // Get all assignments for this course
        List<Assignment> allAssignments = assignmentRepository.findByCourseId(courseId);
        int totalAssignments = allAssignments.size();
        log.info("Total assignments in course: {}", totalAssignments);
        
        if (totalAssignments == 0) {
            enrollment.setProgressPct(BigDecimal.ZERO);
            enrollmentRepository.save(enrollment);
            return;
        }

        // Get graded submissions for this student in this course
        List<Submission> studentSubmissions = submissionRepository.findByStudentId(studentId);
        
        long gradedCount = studentSubmissions.stream()
                .filter(s -> s.getAssignment().getModule().getCourse().getId().equals(courseId))
                .filter(s -> s.getStatus() == SubmissionStatus.GRADED)
                .count();
        
        log.info("Graded assignments count: {}", gradedCount);

        // Calculate progress percentage
        BigDecimal progress = BigDecimal.valueOf(gradedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalAssignments), 2, RoundingMode.HALF_UP);

        enrollment.setProgressPct(progress);
        log.info("Progress calculated: {}%", progress);

        // Auto-complete if 100% done
        if (progress.compareTo(BigDecimal.valueOf(100)) == 0) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            notificationService.sendCourseCompletionNotification(
                    enrollment.getStudent().getEmail(),
                    enrollment.getStudent().getName(),
                    enrollment.getCourse().getName(),
                    enrollment.getGrade() != null
                            ? enrollment.getGrade().toPlainString()
                            : "Pending"
            );
            log.info("Course marked as completed!");
        }

        enrollmentRepository.save(enrollment);
        log.info("Progress saved successfully");
    }

    // calculates and saves the final grade when instructor finalizes
    public void finalizeGrade(Long studentId, Long courseId) {
        log.info("Finalizing grade for studentId={}, courseId={}", studentId, courseId);
        
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Enrollment not found for studentId=" + studentId +
                        ", courseId=" + courseId
                ));

        // Get all graded submissions for this student in this course
        List<Submission> gradedSubmissions = submissionRepository
                .findByStudentId(studentId)
                .stream()
                .filter(s -> s.getAssignment().getModule().getCourse().getId().equals(courseId))
                .filter(s -> s.getScore() != null)
                .collect(Collectors.toList());

        if (gradedSubmissions.isEmpty()) {
            log.warn("No graded submissions found for finalize. studentId={}, courseId={}",
                    studentId, courseId);
            return;
        }

        // Calculate average score percentage
        double totalEarned = 0;
        double totalPossible = 0;
        
        for (Submission submission : gradedSubmissions) {
            totalEarned += submission.getScore().doubleValue();
            totalPossible += submission.getAssignment().getPointsPossible();
        }
        
        double gradePercentage = totalPossible > 0 ? (totalEarned / totalPossible) * 100 : 0;
        BigDecimal finalGrade = BigDecimal.valueOf(gradePercentage).setScale(2, RoundingMode.HALF_UP);

        enrollment.setGrade(finalGrade);
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollmentRepository.save(enrollment);

        log.info("Final grade set: studentId={}, courseId={}, grade={}%",
                studentId, courseId, finalGrade);
        
        // Send completion notification
        notificationService.sendCourseCompletionNotification(
                enrollment.getStudent().getEmail(),
                enrollment.getStudent().getName(),
                enrollment.getCourse().getName(),
                finalGrade.toPlainString()
        );
    }
}