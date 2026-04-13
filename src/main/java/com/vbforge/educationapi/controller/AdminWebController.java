package com.vbforge.educationapi.controller;

import com.vbforge.educationapi.domain.Role;
import com.vbforge.educationapi.domain.Student;
import com.vbforge.educationapi.dto.common.PageResponseDto;
import com.vbforge.educationapi.dto.course.CourseResponseDto;
import com.vbforge.educationapi.dto.student.StudentResponseDto;
import com.vbforge.educationapi.repository.CourseRepository;
import com.vbforge.educationapi.repository.EnrollmentRepository;
import com.vbforge.educationapi.repository.StudentRepository;
import com.vbforge.educationapi.repository.SubmissionRepository;
import com.vbforge.educationapi.service.CourseService;
import com.vbforge.educationapi.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminWebController {

    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseService courseService;
    private final PasswordEncoder passwordEncoder;

    // ============================================
    // DASHBOARD
    // ============================================
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            long totalStudents = studentRepository.count();
            long totalCourses = courseRepository.count();
            long totalEnrollments = enrollmentRepository.count();
            long totalSubmissions = submissionRepository.count();
            long pendingGrading = submissionRepository.findAll().stream()
                    .filter(s -> s.getStatus().toString().equals("SUBMITTED"))
                    .count();
            
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("totalCourses", totalCourses);
            model.addAttribute("totalEnrollments", totalEnrollments);
            model.addAttribute("totalSubmissions", totalSubmissions);
            model.addAttribute("pendingGrading", pendingGrading);
            model.addAttribute("title", "Admin Dashboard");
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        
        return "admin-dashboard";
    }

    // ============================================
    // USER MANAGEMENT
    // ============================================
    
    @GetMapping("/users")
    public String manageUsers(@RequestParam(required = false) String keyword,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            PageResponseDto<StudentResponseDto> studentPage;
            
            if (keyword != null && !keyword.isBlank()) {
                studentPage = studentService.search(keyword, pageable);
                model.addAttribute("keyword", keyword);
            } else {
                studentPage = studentService.findAll(pageable);
            }
            
            List<Map<String, Object>> users = new ArrayList<>();
            for (StudentResponseDto student : studentPage.getContent()) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", student.getId());
                userMap.put("name", student.getName());
                userMap.put("email", student.getEmail());
                userMap.put("role", student.getRole().name());
                userMap.put("enrollmentCount", student.getEnrollmentCount());
                userMap.put("createdAt", student.getCreatedAt());
                users.add(userMap);
            }
            
            model.addAttribute("users", users);
            model.addAttribute("page", studentPage);
            model.addAttribute("title", "Manage Users");
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("users", new ArrayList<>());
        }
        
        return "admin-users";
    }
    
    @GetMapping("/users/create")
    public String showCreateUserForm(Model model) {
        model.addAttribute("roles", Role.values());
        model.addAttribute("title", "Create User");
        return "admin-user-form";
    }
    
    @PostMapping("/users/create")
    public String createUser(@RequestParam String name,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String role,
                             RedirectAttributes redirectAttributes) {
        try {
            if (studentRepository.existsByEmail(email)) {
                redirectAttributes.addFlashAttribute("error", "Email already exists!");
                return "redirect:/admin/users/create";
            }
            
            Student student = Student.builder()
                    .name(name)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .role(Role.valueOf(role))
                    .build();
            
            studentRepository.save(student);
            redirectAttributes.addFlashAttribute("message", "User created successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create user: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
    
    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        try {
            Student student = studentService.getStudentOrThrow(id);
            model.addAttribute("user", student);
            model.addAttribute("roles", Role.values());
            model.addAttribute("title", "Edit User - " + student.getName());
            return "admin-user-edit";
        } catch (Exception e) {
            return "redirect:/admin/users";
        }
    }
    
    @PostMapping("/users/{id}/edit")
    public String updateUser(@PathVariable Long id,
                             @RequestParam String name,
                             @RequestParam String email,
                             @RequestParam(required = false) String password,
                             @RequestParam String role,
                             RedirectAttributes redirectAttributes) {
        try {
            Student student = studentService.getStudentOrThrow(id);
            student.setName(name);
            student.setEmail(email);
            student.setRole(Role.valueOf(role));
            if (password != null && !password.isEmpty()) {
                student.setPasswordHash(passwordEncoder.encode(password));
            }
            studentRepository.save(student);
            
            redirectAttributes.addFlashAttribute("message", "User updated successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update user: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
    
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            studentRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "User deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
    
    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam String role,
                             RedirectAttributes redirectAttributes) {
        try {
            Student student = studentService.getStudentOrThrow(id);
            student.setRole(Role.valueOf(role));
            studentRepository.save(student);
            redirectAttributes.addFlashAttribute("message", "Role updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update role: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ============================================
    // COURSE MANAGEMENT (Admin)
    // ============================================
    
    @GetMapping("/courses")
    public String allCourses(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            var coursePage = courseService.findAll(pageable);
            
            List<Map<String, Object>> courses = new ArrayList<>();
            for (CourseResponseDto course : coursePage.getContent()) {
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("id", course.getId());
                courseMap.put("name", course.getName());
                courseMap.put("instructor", course.getInstructor());
                courseMap.put("moduleCount", course.getModuleCount());
                courseMap.put("enrollmentCount", course.getEnrollmentCount());
                courses.add(courseMap);
            }
            
            model.addAttribute("courses", courses);
            model.addAttribute("page", coursePage);
            model.addAttribute("title", "All Courses");
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        
        return "admin-courses";
    }
    
    @PostMapping("/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Course deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete course: " + e.getMessage());
        }
        return "redirect:/admin/courses";
    }
}