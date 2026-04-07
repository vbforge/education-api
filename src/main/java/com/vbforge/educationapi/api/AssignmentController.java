package com.vbforge.educationapi.api;

import com.vbforge.educationapi.dto.assignment.AssignmentRequestDto;
import com.vbforge.educationapi.dto.assignment.AssignmentResponseDto;
import com.vbforge.educationapi.service.AssignmentService;
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
public class AssignmentController {

    private final AssignmentService assignmentService;

    // GET /api/v1/modules/{moduleId}/assignments
    @GetMapping("/modules/{moduleId}/assignments")
    public ResponseEntity<List<AssignmentResponseDto>> getByModule(@PathVariable Long moduleId) {
        return ResponseEntity.ok(assignmentService.findByModule(moduleId));
    }

    // GET /api/v1/courses/{courseId}/assignments
    @GetMapping("/courses/{courseId}/assignments")
    public ResponseEntity<List<AssignmentResponseDto>> getByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(assignmentService.findByCourse(courseId));
    }

    // GET /api/v1/assignments/{id}
    @GetMapping("/assignments/{id}")
    public ResponseEntity<AssignmentResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.findById(id));
    }

    // POST /api/v1/assignments
    @PostMapping("/assignments")
    public ResponseEntity<AssignmentResponseDto> create(
            @Valid @RequestBody AssignmentRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.create(dto));
    }

    // PUT /api/v1/assignments/{id}
    @PutMapping("/assignments/{id}")
    public ResponseEntity<AssignmentResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRequestDto dto
    ) {
        return ResponseEntity.ok(assignmentService.update(id, dto));
    }

    // DELETE /api/v1/assignments/{id}
    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}