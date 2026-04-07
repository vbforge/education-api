package com.vbforge.educationapi.service;

import com.vbforge.educationapi.domain.Course;
import com.vbforge.educationapi.dto.common.PageResponseDto;
import com.vbforge.educationapi.dto.course.CourseRequestDto;
import com.vbforge.educationapi.dto.course.CourseResponseDto;
import com.vbforge.educationapi.exception.DuplicateResourceException;
import com.vbforge.educationapi.exception.ResourceNotFoundException;
import com.vbforge.educationapi.mapper.CourseMapper;
import com.vbforge.educationapi.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;

    // ── Read ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponseDto<CourseResponseDto> findAll(Pageable pageable) {
        Page<Course> page = courseRepository.findAll(pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<CourseResponseDto> search(String keyword, Pageable pageable) {
        Page<Course> page = courseRepository.search(keyword, pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public CourseResponseDto findById(Long id) {
        Course course = getCourseOrThrow(id);
        return CourseMapper.toDto(course);
    }

    // ── Write ───────────────────────────────────────────────────

    public CourseResponseDto create(CourseRequestDto dto) {
        if (courseRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException(
                    "Course with name '" + dto.getName() + "' already exists"
            );
        }
        Course course = CourseMapper.toEntity(dto);
        return CourseMapper.toDto(courseRepository.save(course));
    }

    public CourseResponseDto update(Long id, CourseRequestDto dto) {
        Course course = getCourseOrThrow(id);

        // allow same name if it belongs to this course (updating other fields)
        boolean nameConflict = courseRepository.existsByNameIgnoreCase(dto.getName())
                && !course.getName().equalsIgnoreCase(dto.getName());
        if (nameConflict) {
            throw new DuplicateResourceException(
                    "Course with name '" + dto.getName() + "' already exists"
            );
        }

        CourseMapper.updateEntity(course, dto);
        return CourseMapper.toDto(courseRepository.save(course));
    }

    public void delete(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course", id);
        }
        courseRepository.deleteById(id);
        // cascade = ALL on modules and enrollments handles children automatically
    }

    // ── Private helpers ─────────────────────────────────────────

    private Course getCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
    }

    private PageResponseDto<CourseResponseDto> toPageResponse(Page<Course> page) {
        List<CourseResponseDto> content = page.getContent()
                .stream()
                .map(CourseMapper::toDto)
                .toList();
        return PageResponseDto.<CourseResponseDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}