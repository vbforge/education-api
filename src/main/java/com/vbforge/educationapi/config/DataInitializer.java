package com.vbforge.educationapi.config;

import com.vbforge.educationapi.domain.Role;
import com.vbforge.educationapi.domain.Student;
import com.vbforge.educationapi.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "test"})          // never runs in docker/prod
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final PasswordEncoder   passwordEncoder;

    @Override
    public void run(String... args) {
        createUserIfNotExists("Admin User",      "admin@email.com",      "admin1234",      Role.ADMIN);
        createUserIfNotExists("Instructor One",  "instructor@email.com", "instructor1234", Role.INSTRUCTOR);
        createUserIfNotExists("Student One",     "student@email.com",    "student1234",    Role.STUDENT);
        log.info("=== Seed users ready ===");
        log.info("  admin@email.com      / admin1234");
        log.info("  instructor@email.com / instructor1234");
        log.info("  student@email.com    / student1234");
    }

    private void createUserIfNotExists(String name, String email, String password, Role role) {
        if (!studentRepository.existsByEmail(email)) {
            studentRepository.save(
                    Student.builder()
                            .name(name)
                            .email(email)
                            .passwordHash(passwordEncoder.encode(password))
                            .role(role)
                            .build()
            );
        }
    }
}