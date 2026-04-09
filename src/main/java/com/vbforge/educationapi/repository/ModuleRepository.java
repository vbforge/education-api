package com.vbforge.educationapi.repository;

import com.vbforge.educationapi.domain.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    // all modules for a course, ordered by position
    List<Module> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    // how many modules does a course have — useful for progress calculation
    int countByCourseId(Long courseId);

    // check for duplicate title within the same course
    boolean existsByCourseIdAndTitleIgnoreCase(Long courseId, String title);

    @Query("SELECT COUNT(a) FROM Assignment a WHERE a.module.id = :moduleId")
    int countAssignmentsByModuleId(@Param("moduleId") Long moduleId);

}
