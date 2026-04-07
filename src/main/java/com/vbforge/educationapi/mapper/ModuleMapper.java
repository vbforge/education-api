package com.vbforge.educationapi.mapper;

import com.vbforge.educationapi.domain.Module;
import com.vbforge.educationapi.dto.module.ModuleRequestDto;
import com.vbforge.educationapi.dto.module.ModuleResponseDto;

public class ModuleMapper {

    private ModuleMapper() {}

    public static ModuleResponseDto toDto(Module module) {
        return ModuleResponseDto.builder()
                .id(module.getId())
                .courseId(module.getCourse().getId())
                .courseName(module.getCourse().getName())
                .title(module.getTitle())
                .content(module.getContent())
                .orderIndex(module.getOrderIndex())
                .assignmentCount(module.getAssignments().size())
                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt())
                .build();
    }

    public static void updateEntity(Module module, ModuleRequestDto dto) {
        module.setTitle(dto.getTitle());
        module.setContent(dto.getContent());
        module.setOrderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : 0);
    }
}