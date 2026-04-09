package com.vbforge.educationapi.service;

import com.vbforge.educationapi.domain.EnrollmentStatus;
import com.vbforge.educationapi.domain.Student;
import com.vbforge.educationapi.dto.common.PageResponseDto;
import com.vbforge.educationapi.dto.enrollment.EnrollmentResponseDto;
import com.vbforge.educationapi.dto.student.StudentProgressResponseDto;
import com.vbforge.educationapi.dto.student.StudentRequestDto;
import com.vbforge.educationapi.dto.student.StudentResponseDto;
import com.vbforge.educationapi.exception.DuplicateResourceException;
import com.vbforge.educationapi.exception.ResourceNotFoundException;
import com.vbforge.educationapi.mapper.EnrollmentMapper;
import com.vbforge.educationapi.mapper.StudentMapper;
import com.vbforge.educationapi.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponseDto<StudentResponseDto> findAll(Pageable pageable) {
        Page<Student> page = studentRepository.findAll(pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<StudentResponseDto> search(String keyword, Pageable pageable) {
        Page<Student> page = studentRepository.search(keyword, pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public StudentResponseDto findById(Long id) {
        Student student = getStudentOrThrow(id);
        int enrollmentCount = studentRepository.countEnrollmentsByStudentId(id);
        return StudentMapper.toDto(student, enrollmentCount);
    }

    public StudentResponseDto register(StudentRequestDto dto) {
        if (studentRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException(
                    "Student with email '" + dto.getEmail() + "' already exists"
            );
        }
        Student student = Student.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .build();

        Student saved = studentRepository.save(student);
        return StudentMapper.toDto(saved, 0);
    }

    public StudentResponseDto update(Long id, StudentRequestDto dto) {
        Student student = getStudentOrThrow(id);

        boolean emailConflict = studentRepository.existsByEmail(dto.getEmail())
                && !student.getEmail().equalsIgnoreCase(dto.getEmail());
        if (emailConflict) {
            throw new DuplicateResourceException(
                    "Email '" + dto.getEmail() + "' is already taken"
            );
        }

        student.setName(dto.getName());
        student.setEmail(dto.getEmail());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            student.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        Student updated = studentRepository.save(student);
        int enrollmentCount = studentRepository.countEnrollmentsByStudentId(id);
        return StudentMapper.toDto(updated, enrollmentCount);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student", id);
        }
        studentRepository.deleteById(id);
    }

    Student getStudentOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    private PageResponseDto<StudentResponseDto> toPageResponse(Page<Student> page) {
        List<StudentResponseDto> content = page.getContent()
                .stream()
                .map(student -> {
                    int enrollmentCount = studentRepository.countEnrollmentsByStudentId(student.getId());
                    return StudentMapper.toDto(student, enrollmentCount);
                })
                .toList();

        return PageResponseDto.<StudentResponseDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public StudentProgressResponseDto getProgress(Long studentId) {
        Student student = getStudentOrThrow(studentId);

        List<EnrollmentResponseDto> enrollments =
                student.getEnrollments()
                        .stream()
                        .map(EnrollmentMapper::toDto)
                        .toList();

        long completed = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();

        long active = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .count();

        return StudentProgressResponseDto.builder()
                .studentId(studentId)
                .studentName(student.getName())
                .totalEnrollments(enrollments.size())
                .completedCourses((int) completed)
                .activeCourses((int) active)
                .enrollments(enrollments)
                .build();
    }

    //--------------------------- web ---------------------------------------


    public Student getStudentOrThrowByEmail(String email) {
        return studentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student with email: " + email));
    }

}