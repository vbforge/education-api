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
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElse(null);

        if (enrollment == null || enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            log.warn("Skipping progress recalc — enrollment not found or dropped. " +
                     "studentId={}, courseId={}", studentId, courseId);
            return;
        }

        int total  = assignmentRepository.findByCourseId(courseId).size();
        int graded = submissionRepository.countGradedByStudentAndCourse(studentId, courseId);

        // avoid division by zero
        BigDecimal progress = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(graded)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

        enrollment.setProgressPct(progress);

        // auto-complete if 100% done
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
        }

        enrollmentRepository.save(enrollment);
        log.debug("Progress updated: studentId={}, courseId={}, progress={}%",
                studentId, courseId, progress);
    }

//    public void recalculate(Long studentId, Long courseId) {
//        Enrollment enrollment = enrollmentRepository
//                .findByStudentIdAndCourseId(studentId, courseId)
//                .orElse(null);
//
//        if (enrollment == null || enrollment.getStatus() == EnrollmentStatus.DROPPED) {
//            log.warn("Skipping progress recalc — enrollment not found or dropped. " +
//                    "studentId={}, courseId={}", studentId, courseId);
//            return;
//        }
//
//        // Get all assignments for this course
//        List<Assignment> assignments = assignmentRepository.findByCourseId(courseId);
//        int totalAssignments = assignments.size();
//
//        // Get graded submissions for this student in this course
//        List<Submission> gradedSubmissions = submissionRepository.findByStudentId(studentId)
//                .stream()
//                .filter(s -> s.getAssignment().getModule().getCourse().getId().equals(courseId))
//                .filter(s -> s.getStatus() == SubmissionStatus.GRADED)
//                .collect(Collectors.toList());
//
//        int gradedCount = gradedSubmissions.size();
//
//        // Calculate progress percentage
//        BigDecimal progress = totalAssignments == 0
//                ? BigDecimal.ZERO
//                : BigDecimal.valueOf(gradedCount)
//                  .divide(BigDecimal.valueOf(totalAssignments), 4, RoundingMode.HALF_UP)
//                  .multiply(BigDecimal.valueOf(100))
//                  .setScale(2, RoundingMode.HALF_UP);
//
//        enrollment.setProgressPct(progress);
//
//        // Calculate average grade if all assignments are graded
//        if (gradedCount == totalAssignments && totalAssignments > 0) {
//            double avgScore = gradedSubmissions.stream()
//                    .mapToDouble(s -> s.getScore().doubleValue())
//                    .average()
//                    .orElse(0);
//            double maxPoints = assignments.stream()
//                    .mapToDouble(a -> a.getPointsPossible())
//                    .sum();
//            double gradePercentage = maxPoints > 0 ? (avgScore / maxPoints) * 100 : 0;
//            enrollment.setGrade(BigDecimal.valueOf(gradePercentage).setScale(2, RoundingMode.HALF_UP));
//            enrollment.setStatus(EnrollmentStatus.COMPLETED);
//
//            // Send notification
//            notificationService.sendCourseCompletionNotification(
//                    enrollment.getStudent().getEmail(),
//                    enrollment.getStudent().getName(),
//                    enrollment.getCourse().getName(),
//                    enrollment.getGrade().toPlainString()
//            );
//        }
//
//        enrollmentRepository.save(enrollment);
//        log.info("Progress updated: studentId={}, courseId={}, progress={}% ({} of {} assignments graded)",
//                studentId, courseId, progress, gradedCount, totalAssignments);
//    }

    // calculates and saves the final grade when instructor finalizes
    public void finalize(Long studentId, Long courseId) {
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Enrollment not found for studentId=" + studentId +
                        ", courseId=" + courseId
                ));

        // average of all scored submissions for this student in this course
        List<BigDecimal> scores = submissionRepository
                .findByStudentId(studentId)
                .stream()
                .filter(s -> s.getAssignment().getModule().getCourse().getId().equals(courseId))
                .filter(s -> s.getScore() != null)
                .map(s -> s.getScore())
                .toList();

        if (scores.isEmpty()) {
            log.warn("No graded submissions found for finalize. studentId={}, courseId={}",
                    studentId, courseId);
            return;
        }

        BigDecimal average = scores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);

        enrollment.setGrade(average);
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollmentRepository.save(enrollment);

        log.info("Final grade set: studentId={}, courseId={}, grade={}",
                studentId, courseId, average);
    }
}