package com.vbforge.educationapi.repository;

import com.vbforge.educationapi.domain.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByCourseIdOrderByPostedAtDesc(Long courseId);
    
    @Query("SELECT a FROM Announcement a WHERE a.course.id = :courseId ORDER BY a.postedAt DESC")
    List<Announcement> findRecentByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(e) > 0 FROM Enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId")
    boolean isStudentEnrolled(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Query(value = "SELECT a.title, a.message, c.name as course_name, a.posted_at " +
            "FROM announcements a " +
            "JOIN courses c ON c.id = a.course_id " +
            "JOIN enrollments e ON e.course_id = c.id " +
            "WHERE e.student_id = :studentId " +
            "ORDER BY a.posted_at DESC",
            nativeQuery = true)
    List<Object[]> findAnnouncementsForStudentNative(@Param("studentId") Long studentId);
}