package com.vbforge.educationapi.service;

import com.vbforge.educationapi.domain.*;
import com.vbforge.educationapi.dto.enrollment.EnrollmentRequestDto;
import com.vbforge.educationapi.dto.enrollment.EnrollmentResponseDto;
import com.vbforge.educationapi.exception.BusinessException;
import com.vbforge.educationapi.exception.ResourceNotFoundException;
import com.vbforge.educationapi.mapper.EnrollmentMapper;
import com.vbforge.educationapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository    studentRepository;
    private final CourseRepository     courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final ProgressService progressService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<EnrollmentResponseDto> findByStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student", studentId);
        }
        return enrollmentRepository.findByStudentId(studentId)
                .stream()
                .map(EnrollmentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponseDto> findByCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course", courseId);
        }
        return enrollmentRepository.findByCourseId(courseId)
                .stream()
                .map(EnrollmentMapper::toDto)
                .toList();
    }

    public EnrollmentResponseDto enroll(EnrollmentRequestDto dto) {
        // guard: student exists
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", dto.getStudentId()));

        // guard: course exists
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", dto.getCourseId()));

        // guard: not already enrolled
        if (enrollmentRepository.existsByStudentIdAndCourseId(dto.getStudentId(), dto.getCourseId())) {
            throw new BusinessException(
                    "Student '" + student.getName() + "' is already enrolled in '" + course.getName() + "'"
            );
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .build();


        Enrollment saved = enrollmentRepository.save(enrollment);
        notificationService.sendEnrollmentConfirmation(
                student.getEmail(),
                student.getName(),
                course.getName()
        );
        return EnrollmentMapper.toDto(saved);
    }

    public EnrollmentResponseDto drop(Long studentId, Long courseId) {
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new BusinessException("Enrollment not found"));

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        return EnrollmentMapper.toDto(enrollmentRepository.save(enrollment));
    }

    // called by SubmissionService after grading
    public void recalculateProgress(Long studentId, Long courseId) {
        progressService.recalculate(studentId, courseId);
    }

    // instructor manually finalizes a student's course grade
    public EnrollmentResponseDto finalizeGrade(Long studentId, Long courseId) {
        progressService.finalizeGrade(studentId, courseId);  // Make sure this matches the method name

        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        return EnrollmentMapper.toDto(enrollment);
    }

    // update enrollment grade directly (instructor override)
    public EnrollmentResponseDto updateGrade(Long studentId, Long courseId, BigDecimal grade) {
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new BusinessException("Enrollment not found"));

        if (grade.compareTo(BigDecimal.ZERO) < 0 ||
                grade.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Grade must be between 0 and 100");
        }

        enrollment.setGrade(grade);
        return EnrollmentMapper.toDto(enrollmentRepository.save(enrollment));
    }


}
