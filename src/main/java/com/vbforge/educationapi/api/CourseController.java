package com.vbforge.educationapi.api;

import com.vbforge.educationapi.dto.common.PageResponseDto;
import com.vbforge.educationapi.dto.course.CourseRequestDto;
import com.vbforge.educationapi.dto.course.CourseResponseDto;
import com.vbforge.educationapi.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    // GET /api/v1/courses?page=0&size=10&sort=name,asc
    // GET /api/v1/courses?keyword=java&page=0&size=10
    @GetMapping
    public ResponseEntity<PageResponseDto<CourseResponseDto>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc")  String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponseDto<CourseResponseDto> result = (keyword != null && !keyword.isBlank())
                ? courseService.search(keyword, pageable)
                : courseService.findAll(pageable);

        return ResponseEntity.ok(result);
    }

    // GET /api/v1/courses/{id}
    @GetMapping("/{id}")
    public ResponseEntity<CourseResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.findById(id));
    }

    // POST /api/v1/courses
    @PostMapping
    public ResponseEntity<CourseResponseDto> create(@Valid @RequestBody CourseRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(dto));
    }

    // PUT /api/v1/courses/{id}
    @PutMapping("/{id}")
    public ResponseEntity<CourseResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequestDto dto
    ) {
        return ResponseEntity.ok(courseService.update(id, dto));
    }

    // DELETE /api/v1/courses/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();   // 204
    }
}