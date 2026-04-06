package com.vbforge.educationapi.repository;

import com.vbforge.educationapi.domain.Submission;
import com.vbforge.educationapi.domain.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    // find a student's specific submission — for display and grading
    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    // has this student already submitted? — guard before saving a new one
    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    // all submissions for an assignment — for instructor grading view
    List<Submission> findByAssignmentId(Long assignmentId);

    // all submissions by a student — for student history
    List<Submission> findByStudentId(Long studentId);

    // all ungraded submissions for assignments in a course — instructor workload
    @Query("""
            SELECT s FROM Submission s
            JOIN s.assignment a
            JOIN a.module m
            WHERE m.course.id = :courseId
              AND s.status = :status
            """)
    List<Submission> findByCourseIdAndStatus(
            @Param("courseId") Long courseId,
            @Param("status") SubmissionStatus status
    );

    // count graded submissions for a student in a course — for progress tracking
    @Query("""
            SELECT COUNT(s) FROM Submission s
            JOIN s.assignment a
            JOIN a.module m
            WHERE m.course.id = :courseId
              AND s.student.id = :studentId
              AND s.status = 'GRADED'
            """)
    int countGradedByStudentAndCourse(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId
    );

}
