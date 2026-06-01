package com.vbforge.educationapi.service;

import com.vbforge.educationapi.domain.Course;
import com.vbforge.educationapi.dto.course.CourseRequestDto;
import com.vbforge.educationapi.dto.course.CourseResponseDto;
import com.vbforge.educationapi.exception.DuplicateResourceException;
import com.vbforge.educationapi.exception.ResourceNotFoundException;
import com.vbforge.educationapi.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock(lenient = true)
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    private Course testCourse;
    private CourseRequestDto testDto;

    @BeforeEach
    void setUp() {
        // Create Course without explicitly setting ID (BaseEntity handles it)
        testCourse = Course.builder()
                .name("Java Programming")
                .description("Learn Java from scratch")
                .instructor("instructor@email.com")
                .build();
        
        // Use reflection to set ID if needed for mocking
        // In tests, we can use setId() method from BaseEntity
        testCourse.setId(1L);
        testCourse.setCreatedAt(LocalDateTime.now());
        testCourse.setUpdatedAt(LocalDateTime.now());

        testDto = new CourseRequestDto();
        testDto.setName("Java Programming");
        testDto.setDescription("Learn Java from scratch");
        testDto.setInstructor("instructor@email.com");
    }

    @Test
    void createCourse_Success() {
        when(courseRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);
        when(courseRepository.countModulesByCourseId(anyLong())).thenReturn(0);
        when(courseRepository.countEnrollmentsByCourseId(anyLong())).thenReturn(0);

        CourseResponseDto result = courseService.create(testDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Java Programming");
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void createCourse_DuplicateName_ThrowsException() {
        when(courseRepository.existsByNameIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> courseService.create(testDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void findById_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(courseRepository.countModulesByCourseId(1L)).thenReturn(2);
        when(courseRepository.countEnrollmentsByCourseId(1L)).thenReturn(5);

        CourseResponseDto result = courseService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Java Programming");
    }

    @Test
    void findById_NotFound_ThrowsException() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    void findAll_ReturnsPageOfCourses() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Course> coursePage = new PageImpl<>(List.of(testCourse));
        
        when(courseRepository.findAll(pageable)).thenReturn(coursePage);
        when(courseRepository.countModulesByCourseId(anyLong())).thenReturn(2);
        when(courseRepository.countEnrollmentsByCourseId(anyLong())).thenReturn(5);

        var result = courseService.findAll(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void updateCourse_Success() {
        Course updatedCourse = Course.builder()
                .name("Advanced Java")
                .description("Updated description")
                .instructor("instructor@email.com")
                .build();
        updatedCourse.setId(1L);
        
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(courseRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);
        when(courseRepository.countModulesByCourseId(1L)).thenReturn(2);
        when(courseRepository.countEnrollmentsByCourseId(1L)).thenReturn(5);

        CourseRequestDto updateDto = new CourseRequestDto();
        updateDto.setName("Advanced Java");
        updateDto.setDescription("Updated description");

        CourseResponseDto result = courseService.update(1L, updateDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Advanced Java");
    }

    @Test
    void deleteCourse_Success() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        doNothing().when(courseRepository).deleteById(1L);

        courseService.delete(1L);

        verify(courseRepository).deleteById(1L);
    }
}