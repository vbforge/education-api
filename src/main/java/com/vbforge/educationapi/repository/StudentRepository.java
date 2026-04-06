package com.vbforge.educationapi.repository;

import com.vbforge.educationapi.domain.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // used by Spring Security to load user by email (login)
    Optional<Student> findByEmail(String email);

    // check email uniqueness before registration
    boolean existsByEmail(String email);

    // paginated + search — for the admin students list
    @Query("""
            SELECT s FROM Student s
            WHERE LOWER(s.name)  LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Student> search(@Param("keyword") String keyword, Pageable pageable);

}
