package com.vbforge.educationapi.api;

import com.vbforge.educationapi.dto.module.ModuleRequestDto;
import com.vbforge.educationapi.dto.module.ModuleResponseDto;
import com.vbforge.educationapi.service.ModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class ModuleController {

    private final ModuleService moduleService;

    // GET /api/v1/courses/{courseId}/modules
    @GetMapping("/courses/{courseId}/modules")
    public ResponseEntity<List<ModuleResponseDto>> getByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(moduleService.findByCourse(courseId));
    }

    // GET /api/v1/modules/{id}
    @GetMapping("/modules/{id}")
    public ResponseEntity<ModuleResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(moduleService.findById(id));
    }

    // POST /api/v1/modules
    @PostMapping("/modules")
    public ResponseEntity<ModuleResponseDto> create(@Valid @RequestBody ModuleRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moduleService.create(dto));
    }

    // PUT /api/v1/modules/{id}
    @PutMapping("/modules/{id}")
    public ResponseEntity<ModuleResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ModuleRequestDto dto
    ) {
        return ResponseEntity.ok(moduleService.update(id, dto));
    }

    // DELETE /api/v1/modules/{id}
    @DeleteMapping("/modules/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        moduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}