package com.vbforge.educationapi.service;

import com.vbforge.educationapi.domain.Assignment;
import com.vbforge.educationapi.domain.Module;
import com.vbforge.educationapi.dto.assignment.AssignmentRequestDto;
import com.vbforge.educationapi.dto.assignment.AssignmentResponseDto;
import com.vbforge.educationapi.exception.ResourceNotFoundException;
import com.vbforge.educationapi.mapper.AssignmentMapper;
import com.vbforge.educationapi.repository.AssignmentRepository;
import com.vbforge.educationapi.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ModuleRepository moduleRepository;

    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> findByModule(Long moduleId) {
        if (!moduleRepository.existsById(moduleId)) {
            throw new ResourceNotFoundException("Module", moduleId);
        }
        return assignmentRepository.findByModuleIdOrderByDueDateAsc(moduleId)
                .stream()
                .map(AssignmentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> findByCourse(Long courseId) {
        return assignmentRepository.findByCourseId(courseId)
                .stream()
                .map(AssignmentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponseDto findById(Long id) {
        return AssignmentMapper.toDto(getAssignmentOrThrow(id));
    }

    public AssignmentResponseDto create(AssignmentRequestDto dto) {
        Module module = moduleRepository.findById(dto.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Module", dto.getModuleId()));

        Assignment assignment = Assignment.builder()
                .module(module)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .pointsPossible(dto.getPointsPossible() != null ? dto.getPointsPossible() : 100)
                .build();

        return AssignmentMapper.toDto(assignmentRepository.save(assignment));
    }

    public AssignmentResponseDto update(Long id, AssignmentRequestDto dto) {
        Assignment assignment = getAssignmentOrThrow(id);
        AssignmentMapper.updateEntity(assignment, dto);
        return AssignmentMapper.toDto(assignmentRepository.save(assignment));
    }

    public void delete(Long id) {
        if (!assignmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Assignment", id);
        }
        assignmentRepository.deleteById(id);
    }

    private Assignment getAssignmentOrThrow(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
    }
}