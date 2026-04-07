package com.vbforge.educationapi.api;

import com.vbforge.educationapi.dto.common.PageResponseDto;
import com.vbforge.educationapi.dto.student.StudentProgressResponseDto;
import com.vbforge.educationapi.dto.student.StudentRequestDto;
import com.vbforge.educationapi.dto.student.StudentResponseDto;
import com.vbforge.educationapi.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Validated
public class StudentController {

    private final StudentService studentService;

    // GET /api/v1/students?page=0&size=10
    // GET /api/v1/students?keyword=john&page=0&size=10
    @GetMapping
    public ResponseEntity<PageResponseDto<StudentResponseDto>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc")  String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponseDto<StudentResponseDto> result = (keyword != null && !keyword.isBlank())
                ? studentService.search(keyword, pageable)
                : studentService.findAll(pageable);

        return ResponseEntity.ok(result);
    }

    // GET /api/v1/students/{id}
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.findById(id));
    }

    // POST /api/v1/students/register
    @PostMapping("/register")
    public ResponseEntity<StudentResponseDto> register(
            @Valid @RequestBody StudentRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.register(dto));
    }

    // PUT /api/v1/students/{id}
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequestDto dto
    ) {
        return ResponseEntity.ok(studentService.update(id, dto));
    }

    // DELETE /api/v1/students/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/students/{id}/progress
    @GetMapping("/{id}/progress")
    public ResponseEntity<StudentProgressResponseDto> getProgress(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getProgress(id));
    }
}