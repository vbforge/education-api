package com.vbforge.educationapi.repository;

import com.vbforge.educationapi.domain.Enrollment;
import com.vbforge.educationapi.domain.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // find a specific enrollment — for updates and checks
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    // is this student already enrolled? — guard before enrolling
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    // all enrollments for a student — for the student dashboard
    List<Enrollment> findByStudentId(Long studentId);

    // all enrollments for a course — for the instructor view
    List<Enrollment> findByCourseId(Long courseId);

    // count active students in a course — for course stats
    int countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    // average grade across a course — for instructor analytics
    @Query("""
            SELECT AVG(e.grade) FROM Enrollment e
            WHERE e.course.id = :courseId
              AND e.grade IS NOT NULL
            """)
    Double averageGradeByCourseId(@Param("courseId") Long courseId);

}
