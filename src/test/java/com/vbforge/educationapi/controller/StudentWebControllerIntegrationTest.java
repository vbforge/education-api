package com.vbforge.educationapi.controller;

import com.vbforge.educationapi.domain.Role;
import com.vbforge.educationapi.domain.Student;
import com.vbforge.educationapi.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentWebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Student testStudent;

    @BeforeEach
    void setUp() {
        testStudent = Student.builder()
                .name("Test Student")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.STUDENT)
                .build();
        studentRepository.save(testStudent);
    }

    @Test
    @Disabled
    void studentDashboard_WithValidUser_ReturnsDashboardView() throws Exception {
        mockMvc.perform(get("/student/dashboard")
                        .with(user(testStudent.getEmail()).password("password123").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("student-dashboard"));
    }

    @Test
    void courseCatalog_WithValidUser_ReturnsCoursesView() throws Exception {
        mockMvc.perform(get("/student/courses")
                        .with(user(testStudent.getEmail()).password("password123").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("student-courses"));
    }
}