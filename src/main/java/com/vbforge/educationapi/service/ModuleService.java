package com.vbforge.educationapi.service;

import com.vbforge.educationapi.domain.Course;
import com.vbforge.educationapi.domain.Module;
import com.vbforge.educationapi.dto.module.ModuleRequestDto;
import com.vbforge.educationapi.dto.module.ModuleResponseDto;
import com.vbforge.educationapi.exception.BusinessException;
import com.vbforge.educationapi.exception.DuplicateResourceException;
import com.vbforge.educationapi.exception.ResourceNotFoundException;
import com.vbforge.educationapi.mapper.ModuleMapper;
import com.vbforge.educationapi.repository.CourseRepository;
import com.vbforge.educationapi.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<ModuleResponseDto> findByCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course", courseId);
        }
        return moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId)
                .stream()
                .map(module -> {
                    int assignmentCount = moduleRepository.countAssignmentsByModuleId(module.getId());
                    return ModuleMapper.toDto(module, assignmentCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ModuleResponseDto findById(Long id) {
        Module module = getModuleOrThrow(id);
        int assignmentCount = moduleRepository.countAssignmentsByModuleId(id);
        return ModuleMapper.toDto(module, assignmentCount);
    }

    public ModuleResponseDto create(ModuleRequestDto dto) {
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", dto.getCourseId()));

        if (moduleRepository.existsByCourseIdAndTitleIgnoreCase(dto.getCourseId(), dto.getTitle())) {
            throw new DuplicateResourceException(
                    "Module '" + dto.getTitle() + "' already exists in this course"
            );
        }

        Module module = Module.builder()
                .course(course)
                .title(dto.getTitle())
                .content(dto.getContent())
                .orderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : 0)
                .build();

        Module saved = moduleRepository.save(module);
        return ModuleMapper.toDto(saved, 0);
    }

    public ModuleResponseDto update(Long id, ModuleRequestDto dto) {
        Module module = getModuleOrThrow(id);

        boolean titleConflict = moduleRepository
                .existsByCourseIdAndTitleIgnoreCase(module.getCourse().getId(), dto.getTitle())
                && !module.getTitle().equalsIgnoreCase(dto.getTitle());
        if (titleConflict) {
            throw new DuplicateResourceException(
                    "Module '" + dto.getTitle() + "' already exists in this course"
            );
        }

        ModuleMapper.updateEntity(module, dto);
        Module updated = moduleRepository.save(module);
        int assignmentCount = moduleRepository.countAssignmentsByModuleId(id);
        return ModuleMapper.toDto(updated, assignmentCount);
    }

    public void delete(Long id) {
        Module module = getModuleOrThrow(id);
        moduleRepository.delete(module);
    }

    private Module getModuleOrThrow(Long id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module", id));
    }
}