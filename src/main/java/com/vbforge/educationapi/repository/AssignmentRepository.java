package com.vbforge.educationapi.repository;

import com.vbforge.educationapi.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // all assignments for a module, ordered by due date
    List<Assignment> findByModuleIdOrderByDueDateAsc(Long moduleId);

    // all assignments for an entire course (cross-module) — for student dashboard
    @Query("""
            SELECT a FROM Assignment a
            JOIN a.module m
            WHERE m.course.id = :courseId
            ORDER BY a.dueDate ASC
            """)
    List<Assignment> findByCourseId(@Param("courseId") Long courseId);

    // upcoming assignments due before a certain date — for email reminders
    @Query("""
            SELECT a FROM Assignment a
            WHERE a.dueDate BETWEEN :now AND :deadline
            """)
    List<Assignment> findUpcomingDue(
            @Param("now") LocalDateTime now,
            @Param("deadline") LocalDateTime deadline
    );

}
