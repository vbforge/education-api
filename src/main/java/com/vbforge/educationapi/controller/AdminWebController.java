package com.vbforge.educationapi.controller;

import com.vbforge.educationapi.domain.*;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
    
//    @GetMapping("/dashboard")
//    public String dashboard(Model model) {
//        try {
//            long totalStudents = studentRepository.count();
//            long totalCourses = courseRepository.count();
//            long totalEnrollments = enrollmentRepository.count();
//            long totalSubmissions = submissionRepository.count();
//            long pendingGrading = submissionRepository.findAll().stream()
//                    .filter(s -> s.getStatus().toString().equals("SUBMITTED"))
//                    .count();
//
//            model.addAttribute("totalStudents", totalStudents);
//            model.addAttribute("totalCourses", totalCourses);
//            model.addAttribute("totalEnrollments", totalEnrollments);
//            model.addAttribute("totalSubmissions", totalSubmissions);
//            model.addAttribute("pendingGrading", pendingGrading);
//            model.addAttribute("title", "Admin Dashboard");
//
//        } catch (Exception e) {
//            model.addAttribute("error", e.getMessage());
//        }
//
//        return "admin-dashboard";
//    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Basic stats - simple counts
        long totalStudents = studentRepository.count();
        long totalCourses = courseRepository.count();
        long totalEnrollments = enrollmentRepository.count();
        long totalSubmissions = submissionRepository.count();

        // Count instructors (using native query or simple stream)
        long totalInstructors = studentRepository.findAll().stream()
                .filter(s -> s.getRole() == Role.INSTRUCTOR)
                .count();

        // Pending grading
        long pendingGrading = submissionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED)
                .count();

        // Calculate completion rate using stream on fetched data
        List<Enrollment> allEnrollments = enrollmentRepository.findAll();
        long completedEnrollments = allEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();
        double completionRate = allEnrollments.isEmpty() ? 0 :
                (completedEnrollments * 100.0 / allEnrollments.size());

        // New courses this month
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        long newCoursesThisMonth = courseRepository.findAll().stream()
                .filter(c -> c.getCreatedAt().isAfter(oneMonthAgo))
                .count();

        // Average enrollment per course
        double avgEnrollmentPerCourse = totalCourses == 0 ? 0 : (double) totalEnrollments / totalCourses;

        // Top courses by enrollment (fetch with explicit data)
        List<Map<String, Object>> courseEnrollmentData = new ArrayList<>();
        for (Course course : courseRepository.findAll()) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", course.getName());
            data.put("count", enrollmentRepository.findByCourseId(course.getId()).size());
            courseEnrollmentData.add(data);
        }
        courseEnrollmentData.sort((a, b) -> Integer.compare((Integer) b.get("count"), (Integer) a.get("count")));

        List<String> courseNames = courseEnrollmentData.stream().limit(5)
                .map(d -> (String) d.get("name")).collect(Collectors.toList());
        List<Long> enrollmentCounts = courseEnrollmentData.stream().limit(5)
                .map(d -> Long.valueOf((Integer) d.get("count"))).collect(Collectors.toList());

        // Grade distribution
        List<BigDecimal> allGrades = allEnrollments.stream()
                .filter(e -> e.getGrade() != null)
                .map(Enrollment::getGrade)
                .collect(Collectors.toList());

        long excellent = allGrades.stream().filter(g -> g.doubleValue() >= 90).count();
        long good = allGrades.stream().filter(g -> g.doubleValue() >= 75 && g.doubleValue() < 90).count();
        long average = allGrades.stream().filter(g -> g.doubleValue() >= 60 && g.doubleValue() < 75).count();
        long poor = allGrades.stream().filter(g -> g.doubleValue() < 60).count();

        // Monthly activity (last 6 months)
        List<String> months = new ArrayList<>();
        List<Long> newUsers = new ArrayList<>();
        List<Long> newSubmissions = new ArrayList<>();

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM");
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = LocalDateTime.now().minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime end = start.plusMonths(1);
            months.add(start.format(monthFormatter));

            long users = studentRepository.findAll().stream()
                    .filter(s -> s.getCreatedAt().isAfter(start) && s.getCreatedAt().isBefore(end))
                    .count();
            long subs = submissionRepository.findAll().stream()
                    .filter(s -> s.getSubmittedAt() != null && s.getSubmittedAt().isAfter(start) && s.getSubmittedAt().isBefore(end))
                    .count();
            newUsers.add(users);
            newSubmissions.add(subs);
        }

        // Recent activities (using fetched data to avoid lazy loading)
        List<Map<String, String>> recentActivities = new ArrayList<>();

        // Add recent enrollments
        List<Enrollment> recentEnrollments = allEnrollments.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .toList();

        for (Enrollment e : recentEnrollments) {
            Map<String, String> activity = new HashMap<>();
            activity.put("icon", "📚");
            activity.put("title", e.getStudent().getName() + " enrolled in " + e.getCourse().getName());
            activity.put("time", formatTimeAgo(e.getCreatedAt()));
            recentActivities.add(activity);
        }

        // Add recent submissions (fetch eagerly)
        List<Submission> recentSubmissions = submissionRepository.findAll().stream()
                .filter(s -> s.getSubmittedAt() != null)
                .sorted((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()))
                .limit(3)
                .toList();

        for (Submission s : recentSubmissions) {
            Map<String, String> activity = new HashMap<>();
            activity.put("icon", "📝");
            // Access data while still in session
            String studentName = s.getStudent().getName();
            String assignmentTitle = s.getAssignment().getTitle();
            activity.put("title", studentName + " submitted " + assignmentTitle);
            activity.put("time", formatTimeAgo(s.getSubmittedAt()));
            recentActivities.add(activity);
        }

        // Sort by time (most recent first)
        recentActivities.sort((a, b) -> {
            String timeA = a.get("time");
            String timeB = b.get("time");
            // Simple heuristic for sorting
            return timeA.compareTo(timeB);
        });

        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalInstructors", totalInstructors);
        model.addAttribute("totalCourses", totalCourses);
        model.addAttribute("totalEnrollments", totalEnrollments);
        model.addAttribute("totalSubmissions", totalSubmissions);
        model.addAttribute("pendingGrading", pendingGrading);
        model.addAttribute("completionRate", Math.round(completionRate));
        model.addAttribute("newCoursesThisMonth", newCoursesThisMonth);
        model.addAttribute("avgEnrollmentPerCourse", Math.round(avgEnrollmentPerCourse));
        model.addAttribute("courseNames", courseNames);
        model.addAttribute("enrollmentCounts", enrollmentCounts);
        model.addAttribute("gradeRanges", Arrays.asList("90-100%", "75-89%", "60-74%", "Below 60%"));
        model.addAttribute("gradeCounts", Arrays.asList(excellent, good, average, poor));
        model.addAttribute("months", months);
        model.addAttribute("newUsers", newUsers);
        model.addAttribute("newSubmissions", newSubmissions);
        model.addAttribute("recentActivities", recentActivities.stream().limit(8).toList());
        model.addAttribute("title", "Admin Dashboard");

        return "admin-dashboard";
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";
        long minutes = java.time.Duration.between(dateTime, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minutes ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hours ago";
        long days = hours / 24;
        if (days < 7) return days + " days ago";
        return dateTime.toLocalDate().toString();
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