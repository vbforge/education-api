package com.vbforge.educationapi.service;

import com.vbforge.educationapi.domain.*;
import com.vbforge.educationapi.dto.submission.SubmissionRequestDto;
import com.vbforge.educationapi.dto.submission.SubmissionResponseDto;
import com.vbforge.educationapi.exception.BusinessException;
import com.vbforge.educationapi.exception.ResourceNotFoundException;
import com.vbforge.educationapi.mapper.SubmissionMapper;
import com.vbforge.educationapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudentRepository    studentRepository;
    private final EnrollmentService    enrollmentService;   // for progress recalculation
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<SubmissionResponseDto> findByAssignment(Long assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId)
                .stream()
                .map(SubmissionMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponseDto> findByStudent(Long studentId) {
        return submissionRepository.findByStudentId(studentId)
                .stream()
                .map(SubmissionMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubmissionResponseDto findById(Long id) {
        return SubmissionMapper.toDto(getSubmissionOrThrow(id));
    }

    // called when a student submits their work (file upload handled in controller)
    public SubmissionResponseDto submit(SubmissionRequestDto dto, String filePath) {
        // guard: already submitted
        if (submissionRepository.existsByAssignmentIdAndStudentId(
                dto.getAssignmentId(), dto.getStudentId())) {
            throw new BusinessException("You have already submitted this assignment");
        }

        Assignment assignment = assignmentRepository.findById(dto.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", dto.getAssignmentId()));

        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", dto.getStudentId()));

        Submission submission = Submission.builder()
                .assignment(assignment)
                .student(student)
                .submittedAt(LocalDateTime.now())
                .filePath(filePath)
                .status(SubmissionStatus.SUBMITTED)
                .build();

        return SubmissionMapper.toDto(submissionRepository.save(submission));
    }

    // called by instructor to grade a submission
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public SubmissionResponseDto grade(Long submissionId, BigDecimal score, String feedback) {
        Submission submission = getSubmissionOrThrow(submissionId);

        if (score.compareTo(BigDecimal.ZERO) < 0 ||
            score.compareTo(BigDecimal.valueOf(submission.getAssignment().getPointsPossible())) > 0) {
            throw new BusinessException(
                    "Score must be between 0 and " + submission.getAssignment().getPointsPossible()
            );
        }

        submission.setScore(score);
        submission.setFeedback(feedback);
        submission.setStatus(SubmissionStatus.GRADED);
        submissionRepository.save(submission);

        notificationService.sendGradeNotification(
                submission.getStudent().getEmail(),
                submission.getStudent().getName(),
                submission.getAssignment().getTitle(),
                score.toPlainString()
        );

        // recalculate student progress after grading
        Long courseId = submission.getAssignment().getModule().getCourse().getId();
        enrollmentService.recalculateProgress(submission.getStudent().getId(), courseId);

        return SubmissionMapper.toDto(submission);
    }

    private Submission getSubmissionOrThrow(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", id));
    }
}