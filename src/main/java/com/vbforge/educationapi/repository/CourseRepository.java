package com.vbforge.educationapi.repository;

import com.vbforge.educationapi.domain.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // paginated list — for the main courses page
    Page<Course> findAll(Pageable pageable);

    // search by name or instructor (case-insensitive) — for the search bar
    @Query("""
            SELECT c FROM Course c
            WHERE LOWER(c.name)       LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.instructor) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Course> search(@Param("keyword") String keyword, Pageable pageable);

    // check if a course name already exists — for validation
    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT COUNT(m) FROM Module m WHERE m.course.id = :courseId")
    int countModulesByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId")
    int countEnrollmentsByCourseId(@Param("courseId") Long courseId);

}
