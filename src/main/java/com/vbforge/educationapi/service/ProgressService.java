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
        log.info("=== RECALCULATE PROGRESS ===");
        log.info("Student ID: {}, Course ID: {}", studentId, courseId);
        
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElse(null);

        if (enrollment == null) {
            log.warn("Enrollment not found!");
            return;
        }
        
        // Get total assignments in this course
        List<Assignment> assignments = assignmentRepository.findByCourseId(courseId);
        int totalAssignments = assignments.size();
        log.info("Total assignments: {}", totalAssignments);
        
        if (totalAssignments == 0) {
            enrollment.setProgressPct(BigDecimal.ZERO);
            enrollmentRepository.save(enrollment);
            return;
        }
        
        // Count graded submissions for this student in this course
        int gradedCount = submissionRepository.countGradedByStudentAndCourse(studentId, courseId);
        log.info("Graded assignments count: {}", gradedCount);
        
        // Calculate progress percentage
        double progressValue = (gradedCount * 100.0) / totalAssignments;
        BigDecimal progress = BigDecimal.valueOf(progressValue).setScale(2, RoundingMode.HALF_UP);
        
        log.info("Progress calculated: {}%", progress);
        
        enrollment.setProgressPct(progress);
        
        // If all assignments are graded, calculate final grade
        if (gradedCount == totalAssignments && totalAssignments > 0) {
            // Get all graded submissions for this student in this course
            List<Submission> gradedSubmissions = submissionRepository.findByStudentId(studentId)
                    .stream()
                    .filter(s -> s.getAssignment().getModule().getCourse().getId().equals(courseId))
                    .filter(s -> s.getScore() != null)
                    .toList();
            
            double totalEarned = 0;
            double totalPossible = 0;
            
            for (Submission submission : gradedSubmissions) {
                totalEarned += submission.getScore().doubleValue();
                totalPossible += submission.getAssignment().getPointsPossible();
            }
            
            if (totalPossible > 0) {
                double gradePercentage = (totalEarned / totalPossible) * 100;
                BigDecimal finalGrade = BigDecimal.valueOf(gradePercentage).setScale(2, RoundingMode.HALF_UP);
                enrollment.setGrade(finalGrade);
                enrollment.setStatus(EnrollmentStatus.COMPLETED);
                log.info("Course completed! Final grade: {}%", finalGrade);
                
                notificationService.sendCourseCompletionNotification(
                        enrollment.getStudent().getEmail(),
                        enrollment.getStudent().getName(),
                        enrollment.getCourse().getName(),
                        finalGrade.toPlainString()
                );
            }
        }
        
        enrollmentRepository.save(enrollment);
        log.info("Progress saved successfully!");
    }

    // called when instructor manually finalizes a student's course grade
    public void finalizeGrade(Long studentId, Long courseId) {
        log.info("Finalizing grade for studentId={}, courseId={}", studentId, courseId);
        
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Enrollment not found for studentId=" + studentId +
                        ", courseId=" + courseId
                ));

        List<Submission> gradedSubmissions = submissionRepository
                .findByStudentId(studentId)
                .stream()
                .filter(s -> s.getAssignment().getModule().getCourse().getId().equals(courseId))
                .filter(s -> s.getScore() != null)
                .toList();

        if (gradedSubmissions.isEmpty()) {
            log.warn("No graded submissions found for finalize");
            return;
        }

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

        log.info("Final grade set: {}%", finalGrade);
        
        notificationService.sendCourseCompletionNotification(
                enrollment.getStudent().getEmail(),
                enrollment.getStudent().getName(),
                enrollment.getCourse().getName(),
                finalGrade.toPlainString()
        );
    }
}