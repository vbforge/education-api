package com.vbforge.educationapi.mapper;

import com.vbforge.educationapi.domain.Course;
import com.vbforge.educationapi.dto.course.CourseRequestDto;
import com.vbforge.educationapi.dto.course.CourseResponseDto;

public class CourseMapper {

    private CourseMapper() {}   // utility class — no instances

    public static CourseResponseDto toDto(Course course) {
        return CourseResponseDto.builder()
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .instructor(course.getInstructor())
                .syllabus(course.getSyllabus())
                .schedule(course.getSchedule())
                .moduleCount(course.getModules().size())
                .enrollmentCount(course.getEnrollments().size())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    public static Course toEntity(CourseRequestDto dto) {
        return Course.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .instructor(dto.getInstructor())
                .syllabus(dto.getSyllabus())
                .schedule(dto.getSchedule())
                .build();
    }

    public static void updateEntity(Course course, CourseRequestDto dto) {
        course.setName(dto.getName());
        course.setDescription(dto.getDescription());
        course.setInstructor(dto.getInstructor());
        course.setSyllabus(dto.getSyllabus());
        course.setSchedule(dto.getSchedule());
    }
}