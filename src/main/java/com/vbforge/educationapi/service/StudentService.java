package com.vbforge.educationapi.service;

import com.vbforge.educationapi.domain.Student;
import com.vbforge.educationapi.dto.common.PageResponseDto;
import com.vbforge.educationapi.dto.student.StudentRequestDto;
import com.vbforge.educationapi.dto.student.StudentResponseDto;
import com.vbforge.educationapi.exception.DuplicateResourceException;
import com.vbforge.educationapi.exception.ResourceNotFoundException;
import com.vbforge.educationapi.mapper.StudentMapper;
import com.vbforge.educationapi.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;   // injected from SecurityConfig later

    @Transactional(readOnly = true)
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
        return StudentMapper.toDto(getStudentOrThrow(id));
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
                .passwordHash(passwordEncoder.encode(dto.getPassword()))  // hash here!
                .build();

        return StudentMapper.toDto(studentRepository.save(student));
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
        return StudentMapper.toDto(studentRepository.save(student));
    }

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
                .map(StudentMapper::toDto)
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
}