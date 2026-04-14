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
}