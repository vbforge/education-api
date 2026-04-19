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
    @Query("SELECT COUNT(s) FROM Submission s " +
            "WHERE s.student.id = :studentId " +
            "AND s.assignment.module.course.id = :courseId " +
            "AND s.status = 'GRADED'")
    int countGradedByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);


    @Query("SELECT s FROM Submission s WHERE s.assignment.module.course.id = :courseId AND s.status = 'SUBMITTED'")
    List<Submission> findPendingByCourseId(@Param("courseId") Long courseId);

    @Query(value = "SELECT s.id, a.title as assignment_title, st.name as student_name, s.submitted_at " +
            "FROM submissions s " +
            "JOIN assignments a ON a.id = s.assignment_id " +
            "JOIN modules m ON m.id = a.module_id " +
            "JOIN courses c ON c.id = m.course_id " +
            "JOIN students st ON st.id = s.student_id " +
            "WHERE c.id = :courseId AND s.status = 'SUBMITTED'",
            nativeQuery = true)
    List<Object[]> findPendingSubmissionsByCourseIdNative(@Param("courseId") Long courseId);

    @Query(value = "SELECT s.id, a.title as assignment_title, a.points_possible, st.name as student_name, " +
            "c.name as course_name, c.id as course_id, s.submitted_at, s.file_path, s.score, s.feedback " +
            "FROM submissions s " +
            "JOIN assignments a ON a.id = s.assignment_id " +
            "JOIN modules m ON m.id = a.module_id " +
            "JOIN courses c ON c.id = m.course_id " +
            "JOIN students st ON st.id = s.student_id " +
            "WHERE s.id = :submissionId",
            nativeQuery = true)
    List<Object[]> findSubmissionByIdNative(@Param("submissionId") Long submissionId);

    @Query(value = "SELECT a.title, a.due_date, a.points_possible, s.score, s.status, s.file_path " +
            "FROM submissions s " +
            "JOIN assignments a ON a.id = s.assignment_id " +
            "WHERE s.student_id = :studentId " +
            "ORDER BY a.due_date ASC",
            nativeQuery = true)
    List<Object[]> findStudentSubmissionsWithDetailsNative(@Param("studentId") Long studentId);


    @Query(value = "SELECT a.id, a.title, a.due_date, s.score, s.status, c.name as course_name, a.points_possible, s.file_path " +
            "FROM submissions s " +
            "JOIN assignments a ON a.id = s.assignment_id " +
            "JOIN modules m ON m.id = a.module_id " +
            "JOIN courses c ON c.id = m.course_id " +
            "WHERE s.student_id = :studentId " +
            "ORDER BY a.due_date ASC",
            nativeQuery = true)
    List<Object[]> findSubmissionsWithDetailsNative(@Param("studentId") Long studentId);

    @Query(value = "SELECT a.title, c.name, s.score, a.points_possible, s.feedback, s.submitted_at, s.updated_at " +
            "FROM submissions s " +
            "JOIN assignments a ON a.id = s.assignment_id " +
            "JOIN modules m ON m.id = a.module_id " +
            "JOIN courses c ON c.id = m.course_id " +
            "WHERE s.student_id = :studentId AND s.status = 'GRADED' " +
            "ORDER BY s.updated_at DESC",
            nativeQuery = true)
    List<Object[]> findGradedSubmissionsWithDetailsNative(@Param("studentId") Long studentId);


    @Query(value = "SELECT a.id, a.title, a.due_date, NULL as score, 'PENDING' as status, c.name as course_name " +
            "FROM assignments a " +
            "JOIN modules m ON m.id = a.module_id " +
            "JOIN courses c ON c.id = m.course_id " +
            "JOIN enrollments e ON e.course_id = c.id " +
            "WHERE e.student_id = :studentId " +
            "AND a.id NOT IN (SELECT s.assignment_id FROM submissions s WHERE s.student_id = :studentId) " +
            "AND a.due_date > NOW() " +
            "UNION ALL " +
            "SELECT a.id, a.title, a.due_date, s.score, s.status, c.name " +
            "FROM submissions s " +
            "JOIN assignments a ON a.id = s.assignment_id " +
            "JOIN modules m ON m.id = a.module_id " +
            "JOIN courses c ON c.id = m.course_id " +
            "WHERE s.student_id = :studentId AND s.status = 'PENDING' " +
            "ORDER BY due_date ASC",
            nativeQuery = true)
    List<Object[]> findPendingSubmissionsForStudentNative(@Param("studentId") Long studentId);

}
